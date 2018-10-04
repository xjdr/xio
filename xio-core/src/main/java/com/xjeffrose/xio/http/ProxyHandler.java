package com.xjeffrose.xio.http;

import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.SocketAddressHelper;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AsciiString;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class ProxyHandler implements PipelineRequestHandler {
  private static final AsciiString X_FORWARDED_FOR = AsciiString.cached("x-forwarded-for");

  protected final ClientFactory factory;
  protected final ProxyRouteConfig config;
  protected final SocketAddressHelper addressHelper;

  public ProxyHandler(
      ClientFactory factory, ProxyRouteConfig config, SocketAddressHelper addressHelper) {
    this.factory = factory;
    this.config = config;
    this.addressHelper = addressHelper;
  }

  public Optional<ClientConfig> getClientConfig(ChannelHandlerContext ctx, Request request) {
    List<ClientConfig> clientConfigs = config.clientConfigs();
    if (clientConfigs.size() > 0) {
      return Optional.of(clientConfigs.get(0));
    }
    return Optional.empty();
  }

  public String buildProxyHost(Request request, ClientConfig clientConfig) {
    switch (config.proxyHostPolicy()) {
      case UseRequestHeader:
        return request.host();
      case UseRemoteAddress:
        return clientConfig.remote().getHostString() + ":" + clientConfig.remote().getPort();
      case UseConfigValue:
        return config.proxyHost();
      default:
        log.error("Unknown proxyHostPolicy {}", config.proxyHostPolicy());
        return null;
    }
  }

  public String buildProxyPath(Request request, RouteState state) {
    Optional<String> pathSuffix =
        state
            .route()
            .groups(request.path()) // apply the regex
            .entrySet()
            .stream() // stream the entry set of matches
            .filter(e -> e.getKey().equals("path")) // find the entry with key path
            .map(e -> e.getValue()) // extract the value (aka what did the regex match)
            .findFirst(); // extract the first (Optional) result

    return pathSuffix
        .map(config.proxyPath()::concat) // append the path suffix (if it exists)
        .orElse(config.proxyPath()); // use the provided path with no suffix
  }

  public Request buildRequest(Request request, String proxyHost, String path) {

    Request result;

    if (request instanceof FullRequest) {
      result =
          DefaultFullRequest.builder()
              .body(request.body())
              .method(request.method())
              .path(path)
              .streamId(request.streamId())
              .headers(request.headers())
              .httpTraceInfo(request.httpTraceInfo())
              .host(proxyHost)
              .build();
    } else if (request instanceof SegmentedRequest) {
      result =
          DefaultSegmentedRequest.builder()
              .method(request.method())
              .path(path)
              .streamId(request.streamId())
              .headers(request.headers())
              .httpTraceInfo(request.httpTraceInfo())
              .host(proxyHost)
              .build();
    } else { // this should never happen!
      log.error("Unknown request type: {}", request);
      result = null;
    }

    return result;
  }

  protected String getOriginatingAddressAndPort(ChannelHandlerContext ctx, Request request) {
    val rawXFF = request.headers().get(X_FORWARDED_FOR);
    if (rawXFF == null || rawXFF.toString().trim().isEmpty()) {
      return addressHelper.extractRemoteAddressAndPort(ctx.channel());
    } else {
      String stringXFF = rawXFF.toString();
      if (stringXFF.contains(",")) {
        // extract originating address from list of addresses
        return stringXFF.substring(0, stringXFF.indexOf(","));
      } else {
        // XFF only has one address
        return stringXFF;
      }
    }
  }

  private void appendXForwardedFor(ChannelHandlerContext ctx, Request request) {
    val remoteAddressAndPort = addressHelper.extractRemoteAddressAndPort(ctx.channel());
    if (remoteAddressAndPort != null) {
      val rawXFF = request.headers().get(X_FORWARDED_FOR);
      if (rawXFF == null || rawXFF.toString().trim().isEmpty()) {
        request.headers().set(X_FORWARDED_FOR, remoteAddressAndPort);
      } else {
        val newXFF = rawXFF.toString().trim() + ", " + remoteAddressAndPort;
        request.headers().set(X_FORWARDED_FOR, newXFF);
      }
    }
  }

  @Override
  public void handle(ChannelHandlerContext ctx, Request request, RouteState route) {

    // TODO(CK): propagate any incoming tracing span to the outgoing request
    // below is the old deprecated pattern for this.
    /*
    XioRequest request =
        HttpTracingState.hasSpan(ctx)
            ? new XioRequest(payload, HttpTracingState.getSpan(ctx).context())
            : new XioRequest(payload, null);
    */

    // 1) map the incoming request path to the outgoing request path
    // 2) set the outgoing request host
    // 3) set the tracing span (if there is one)

    val clientConfig = getClientConfig(ctx, request);
    if (clientConfig.isPresent()) {
      Client client = factory.getClient(ctx, clientConfig.get());

      if (!request.startOfMessage()) {
        log.debug("not start of stream");
        writeClientRequest(ctx, client, request);
        return;
      }
      log.debug("start of stream");

      String proxyHost = buildProxyHost(request, clientConfig.get());
      Request proxyRequest = buildRequest(request, proxyHost, buildProxyPath(request, route));

      appendXForwardedFor(ctx, proxyRequest);

      writeClientRequest(ctx, client, proxyRequest);
    } else {
      Response notAvailable = ResponseBuilders.newServiceUnavailable(request);
      ctx.writeAndFlush(notAvailable);
    }
  }

  private void writeClientRequest(ChannelHandlerContext ctx, Client client, Request request) {
    Optional<ChannelFuture> optionalFuture = client.write(request);
    optionalFuture.ifPresent(
        channelFuture ->
            channelFuture.addListener(
                (f) -> {
                  if (!f.isSuccess()) {
                    if (request.startOfMessage()) {
                      log.error("proxy request failed client write failed", f.cause());
                      Response notFound = ResponseBuilders.newServiceUnavailable(request);
                      ctx.writeAndFlush(notFound);
                    } else {
                      log.error("proxy request failed client write failed mid stream", f.cause());
                    }
                  }
                }));
    // This scenario occurs when the client that we have has since disconnected from the origin server
    // we should probably throw this client away and get a new one.  Each client is hard baked with the event loop
    // of the server channel that originally spawned it.  This causes issues when the client tries to reconnect on the
    // original eventloop/thread and the server channel is on a different eventloop/thread. The connect and write
    // are serialized by netty, however the write samples the current channelpipeline (which is empty before connect completes)
    // before it gets serially scheduled to be after connect and when it finally gets scheduled to run
    // (after connect builds the pipeline), it will try to write the first request
    // to the HEAD channelhandlercontext which will throw an error. We need to either 1) make the connection manager
    // resilient to this thread swapping or 2) we just get a new client that is still connected or 3) create a new client
    // that is built based on the clientConfig/Eventloop that our current context is on.
    if (!optionalFuture.isPresent()) {
      if (request.startOfMessage()) {
        log.error("proxy request failed - backend client disconnected");
        Response notFound = ResponseBuilders.newNotFound(request);
        ctx.writeAndFlush(notFound);
      } else {
        log.error("proxy request failed - backend client disconnected mid stream");
      }
    }
  }
}
