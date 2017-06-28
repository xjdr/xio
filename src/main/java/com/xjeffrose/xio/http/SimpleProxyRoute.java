package com.xjeffrose.xio.http;

import java.util.Optional;

import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioClientBootstrap;
import com.xjeffrose.xio.server.Route;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleProxyRoute implements RouteProvider {

  private static final AttributeKey<XioClient> key = AttributeKey.newInstance("xio_client");
  private final Route route;
  private final ProxyConfig config;
  private XioClient client;

  public SimpleProxyRoute(Route route, ProxyConfig config) {
    this.config = config;
    this.route = route;
  }

  private void buildAndAttach(ChannelHandlerContext ctx) {
    client = new XioClientBootstrap(ctx.channel().eventLoop())
      .address(config.address)
      .ssl(config.needSSL)
      .applicationProtocol(() -> new HttpClientCodec())
      .handler(new RawBackendHandler(ctx))
      .build()
    ;
    ctx.channel().attr(key).set(client);
  }

  @Override
  public RouteUpdateProvider handle(HttpRequest request, ChannelHandlerContext ctx) {
    ReferenceCountUtil.retain(request);
    buildAndAttach(ctx);
    if (HttpUtil.is100ContinueExpected(request)) {
      ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
    }

    Optional<String> path = route.groups(request.getUri())
      .entrySet()
      .stream()
      .filter(e -> e.getKey().equals("path"))
      .map(e -> e.getValue())
      .findFirst();

    path.map(config.urlPath::concat);

    request.setUri(path.orElse(config.urlPath));

    request.headers().set("Host", config.hostHeader);

    log.info("Requesting {}", request);
    ctx.channel().attr(key).get().write(request);

    return new RouteUpdateProvider() {
      @Override
      public void update(HttpContent content) {
        System.out.println("update");
        ReferenceCountUtil.retain(content);
        client.write(content);
      }
      @Override
      public void update(LastHttpContent last) {
        System.out.println("last update");
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
