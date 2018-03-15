package com.xjeffrose.xio.http;

import com.xjeffrose.xio.client.ClientConfig;
import io.netty.channel.ChannelHandlerContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyHandler implements PipelineRequestHandler {

  protected final ClientFactory factory;
  protected final ProxyRouteConfig config;

  public ProxyHandler(ClientFactory factory, ProxyRouteConfig config) {
    this.factory = factory;
    this.config = config;
  }

  public ClientConfig getClientConfig(Request request) {
    return config.clientConfigs().get(0);
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
              .headers(request.headers())
              .host(proxyHost)
              .build();
    } else if (request instanceof StreamingRequest) {
      result =
          DefaultStreamingRequest.builder()
              .method(request.method())
              .path(path)
              .headers(request.headers())
              .host(proxyHost)
              .build();
    } else { // this should never happen!
      log.error("Unknown request type: {}", request);
      result = null;
    }

    return result;
  }

  private void appendXForwardedFor(ChannelHandlerContext ctx, Request request) {
    // TODO(CK): update request headers
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

    ClientConfig clientConfig = getClientConfig(request);
    Client client = factory.getClient(ctx, clientConfig);

    if (!request.startOfStream()) {
      log.debug("not start of stream");
      client.write(request);
      return;
    }
    log.debug("start of stream");

    String proxyHost = buildProxyHost(request, clientConfig);
    Request proxyRequest = buildRequest(request, proxyHost, buildProxyPath(request, route));

    appendXForwardedFor(ctx, proxyRequest);

    client.write(proxyRequest);
  }
}
