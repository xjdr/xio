package com.xjeffrose.xio.http;

import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioClientBootstrap;
import com.xjeffrose.xio.client.XioRequest;
import com.xjeffrose.xio.server.Route;
import com.xjeffrose.xio.tracing.HttpTracingState;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleProxyRoute implements RouteProvider {

  private static final AttributeKey<XioClient> key = AttributeKey.newInstance("xio_client");
  private final Route route;
  private final ProxyConfig config;
  private final XioClientBootstrap bootstrap;
  private XioClient client;

  public SimpleProxyRoute(Route route, ProxyConfig config, XioClientBootstrap bootstrap) {
    this.config = config;
    this.route = route;
    this.bootstrap = bootstrap;
  }

  public SimpleProxyRoute(Route route, ProxyConfig config) {
    this(route, config, new XioClientBootstrap());
  }

  private void buildAndAttach(ChannelHandlerContext ctx) {

    client = bootstrap.clone(ctx.channel().eventLoop())
      .address(config.address)
      .ssl(config.needSSL)
      .applicationProtocol(() -> new HttpClientCodec())
      .handler(new RawBackendHandler(ctx))
      .build()
    ;
    ctx.channel().attr(key).set(client);
  }

  @Override
  public RouteUpdateProvider handle(HttpRequest payload, ChannelHandlerContext ctx) {
    ReferenceCountUtil.retain(payload);
    buildAndAttach(ctx);
    if (HttpUtil.is100ContinueExpected(payload)) {
      ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
    }

    Optional<String> path = route.groups(payload.getUri())
      .entrySet()
      .stream()
      .filter(e -> e.getKey().equals("path"))
      .map(e -> e.getValue())
      .findFirst();

    payload.setUri(path.map(config.urlPath::concat).orElse(config.urlPath));

    payload.headers().set("Host", config.hostHeader);

    XioRequest request;

    if (HttpTracingState.hasSpan(ctx)) {
      request = new XioRequest(payload, HttpTracingState.getSpan(ctx).context());
    } else {
      request = new XioRequest(payload, null);
    }

    log.info("Requesting {}", payload);
    ctx.channel().attr(key).get().write(request);

    return new RouteUpdateProvider() {
      @Override
      public void update(HttpContent content) {
        ReferenceCountUtil.retain(content);
        client.write(content);
      }
      @Override
      public void update(LastHttpContent last) {
        ReferenceCountUtil.retain(last);
        client.write(last);
      }
    };
  }

  @Override
  public void close() {
    if (client != null) {
      try {
        client.close();
      } catch(java.io.IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
