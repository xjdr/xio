package com.xjeffrose.xio.server.trailhead;

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioClientBootstrap;
import com.xjeffrose.xio.client.loadbalancer.Protocol;
import com.xjeffrose.xio.server.Route;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j;

import java.net.InetSocketAddress;

@Log4j
public class Http1ProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private RouteConfig.ProxyTo determineProxyTo(FullHttpRequest request) {
    for(Route route : routes.keySet()) {
      if (route.matches(request.uri())) {
        return routes.get(route);
      }
    }
    return null;
  }

  private XioClient client;
  private final ImmutableMap<Route, RouteConfig.ProxyTo> routes;

  public Http1ProxyHandler(ImmutableMap<Route, RouteConfig.ProxyTo> routes) {
    this.routes = routes;
  }

  @Override
  public final void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
    FullHttpRequest req = ReferenceCountUtil.retain(msg);

    ctx.pipeline().remove(HttpObjectAggregator.class);
    ctx.pipeline().get(HttpServerCodec.class).removeInboundHandler();
    ctx.pipeline().get(HttpServerCodec.class).removeOutboundHandler();

    RouteConfig.ProxyTo proxyTo = determineProxyTo(req);

    if (proxyTo == null) {
      System.out.println("I GIVE UP!!!");
    } else {
      req.setUri(proxyTo.urlPath);
    }

    req.headers().set("Host", proxyTo.host);

    client = new XioClientBootstrap(ctx.channel().eventLoop())
      .address(proxyTo.address)
      .ssl(proxyTo.needSSL)
      .applicationProtocol(() -> new HttpRequestEncoder())
      .handler(new RawBackendHandler(ctx))
      .build()
    ;

    client.write(req);
  }

  @Override
  public final void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (client != null) {
      client.close();
    }
  }

  @Override
  public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("exceptionCaught", cause);
    ctx.close();
  }

}
