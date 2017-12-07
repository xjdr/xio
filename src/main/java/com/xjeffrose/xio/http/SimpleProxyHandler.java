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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Slf4j
public class SimpleProxyHandler implements RequestHandler {

  private static final AttributeKey<XioClient> key = AttributeKey.newInstance("xio_client");
  @Getter
  private final Route route;
  @Getter
  private final ProxyConfig config;
  private final XioClientBootstrap bootstrap;
  private XioClient client;

  public SimpleProxyHandler(Route route, ProxyConfig config, XioClientBootstrap bootstrap) {
    this.route = route;
    this.config = config;
    this.bootstrap = bootstrap;
  }

  private void buildAndAttach(ChannelHandlerContext ctx) {
    client = bootstrap.clone(ctx.channel().eventLoop())
      .address(config.address)
      .ssl(config.needSSL)
      .applicationProtocol(HttpClientCodec::new)
      .handler(new RawBackendHandler(ctx))
      .build()
    ;
    ctx.channel().attr(key).set(client);
  }

  @Override
  public RequestUpdateHandler handle(HttpRequest payload, ChannelHandlerContext ctx) {
    ReferenceCountUtil.retain(payload);
    buildAndAttach(ctx);
    if (HttpUtil.is100ContinueExpected(payload)) {
      ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
    }

    Optional<String> path = route.groups(payload.uri())
      .entrySet()
      .stream()
      .filter(e -> e.getKey().equals("path"))
      .map(e -> e.getValue())
      .findFirst();

    if (!config.pathPassthru) {
      payload.setUri(path.map(config.urlPath::concat).orElse(config.urlPath));
    }

    payload.headers().set("Host", config.hostHeader);

    XioRequest request =
      HttpTracingState.hasSpan(ctx)
        ? new XioRequest(payload, HttpTracingState.getSpan(ctx).context())
        : new XioRequest(payload, null);

    log.info("Requesting {}", payload);
    ctx.channel().attr(key).get().write(request);

    return new RequestUpdateHandler() {
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
      } catch (java.io.IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public String toString() {
    return String.format("%s:%s:%s", config.address.getHostString(), config.address.getPort(),config.address.getHostName());
  }
}
