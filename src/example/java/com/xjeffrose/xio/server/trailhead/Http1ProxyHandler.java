package com.xjeffrose.xio.server.trailhead;

import com.google.common.collect.ImmutableMap;
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

import java.net.InetSocketAddress;

// TODO(CK): Change this to SimpleChannelInboundHandler<FullHttpRequest>
public class Http1ProxyHandler extends SimpleChannelInboundHandler<Object> {
  private final class BackendHandler extends ChannelInboundHandlerAdapter {

    private final ChannelHandlerContext frontend;

    BackendHandler(ChannelHandlerContext frontend) {
      this.frontend = frontend;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      frontend.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      frontend.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      frontend.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      ctx.close();
    }
  }

  private RouteConfig.ProxyTo determineProxyTo(FullHttpRequest request) {
    for(Route route : routes.keySet()) {
      if (route.matches(request.uri())) {
        return routes.get(route);
      }
    }
    return null;
  }

  private ChannelPipeline maybeSSL(ChannelPipeline pipeline, boolean needSSL) {
    if (needSSL) {
      pipeline.addLast(clientSslCtx.newHandler(PooledByteBufAllocator.DEFAULT));
    }
    return pipeline;
  }
  private ChannelFuture connectToDestination(EventLoop loop, ChannelHandler handler, InetSocketAddress address, boolean needSSL) {
    /*
    XioClient client = XioClientBootstrap.create()
      .channelHandler(handler)
      .group(loop)
      .ssl(needSSL)
      .build()
    ;
    */
    Bootstrap b = new Bootstrap();
    b.channel(NioSocketChannel.class);
    b.group(loop);
    b.handler(new ChannelInitializer() {
      public void initChannel(Channel channel) {
        maybeSSL(channel.pipeline(), needSSL)
          .addLast(handler)
          .addLast(new HttpRequestEncoder())
        ;
      }
    });
    return b.connect(address);
  }

  static final SslContext clientSslCtx;

  static {
    SslContext cctx;
    try {
      cctx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
    } catch (Exception e) {
      throw new Error(e);
    }
    clientSslCtx = cctx;
  }
  private Channel backend;
  private final ImmutableMap<Route, RouteConfig.ProxyTo> routes;

  public Http1ProxyHandler(ImmutableMap<Route, RouteConfig.ProxyTo> routes) {
    this.routes = routes;
  }

  /**
   * This is an FullHttpRequest at this point
   */
  @Override
  public final void channelRead0(final ChannelHandlerContext ctx, Object msg) throws Exception {
    FullHttpRequest req = (FullHttpRequest) ReferenceCountUtil.retain(msg);

    ctx.pipeline().remove(HttpObjectAggregator.class);
    ctx.pipeline().get(HttpServerCodec.class).removeInboundHandler();
    ctx.pipeline().get(HttpServerCodec.class).removeOutboundHandler();

    RouteConfig.ProxyTo proxyTo = determineProxyTo(req);

    if (proxyTo == null) {
      System.out.println("I GIVE UP!!!");
    } else {
      System.out.println("PROXY TO: " + proxyTo.url + " need SSL: " + proxyTo.needSSL);
      System.out.println("PROXY REQ: " + req.uri());
      req.setUri(proxyTo.urlPath);
      System.out.println("PROXY REQ: " + req.uri());
    }

    req.headers().set("Host", proxyTo.host);

    ChannelFuture f = connectToDestination(ctx.channel().eventLoop(), new BackendHandler(ctx), proxyTo.address, proxyTo.needSSL);
    f.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
          ctx.close();
        } else {
          backend = future.channel();
          backend.write(req);
          backend.flush();
        }
      }
    });
  }

  @Override
  public final void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (backend != null) {
      backend.close();
    }
  }

  @Override
  public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }

}
