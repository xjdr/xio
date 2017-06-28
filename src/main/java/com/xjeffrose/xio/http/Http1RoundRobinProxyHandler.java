package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioClientBootstrap;
import com.xjeffrose.xio.client.loadbalancer.Protocol;
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
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class Http1RoundRobinProxyHandler extends SimpleChannelInboundHandler<HttpObject> {

  private final RoundRobinProxyConfig proxyConfig;
  private RouteProvider route;
  private RouteUpdateProvider updater;

  public Http1RoundRobinProxyHandler(RoundRobinProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
  }

  @Override
  public final void channelRead0(final ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest)msg;
      log.info("Received Request {}", req);
      route = proxyConfig.getRouteProvider(req);
      updater = route.handle(req, ctx);
    } else if (msg instanceof LastHttpContent) {
      updater.update((LastHttpContent)msg);
    } else if (msg instanceof HttpContent) {
      updater.update((HttpContent)msg);
    }
  }

  @Override
  public final void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (route != null) {
      route.close();
    }
  }

  @Override
  public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("exceptionCaught", cause);
    ctx.close();
  }

}
