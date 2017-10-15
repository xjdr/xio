package com.xjeffrose.xio.helpers;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.client.ChannelConfiguration;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

public class HttpsProxyServer extends ProxyServer {

  static final SslContext clientSslCtx;

  static {
    SslContext cctx;
    try {
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      cctx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
    } catch (Exception e) {
      throw new Error(e);
    }
    clientSslCtx = cctx;
  }

  URI uri;

  public HttpsProxyServer(URI uri) {
    super(new InetSocketAddress(uri.getHost(), uri.getPort()));
    this.uri = uri;
  }

  public void buildHandlers(ApplicationState appState, XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
    pipeline.addLast(new HttpObjectAggregator(1));
    pipeline.addLast(new HttpIntermediaryHandler());
  }

  private final class HttpIntermediaryHandler extends IntermediaryHandler {

    @Override
    protected boolean handleProxyProtocol(ChannelHandlerContext ctx, Object msg) throws Exception {
      FullHttpRequest req = (FullHttpRequest) msg;

      ctx.pipeline().remove(HttpObjectAggregator.class);
      ctx.pipeline().get(HttpServerCodec.class).removeInboundHandler();

      String host = destination.getHostString() + ":" + destination.getPort();
      req.headers().set("Host", host);
      req.setUri(uri.getRawPath());
      addReceived(req);

      ctx.pipeline().get(HttpServerCodec.class).removeOutboundHandler();
      return true;
    }

    @Override
    protected SocketAddress intermediaryDestination() {
      return destination;
    }

    @Override
    protected ChannelFuture connectToDestination(EventLoop loop, ChannelHandler handler) {
      ChannelConfiguration config = ChannelConfiguration.clientConfig(loop);

      Bootstrap b = new Bootstrap();
      b.channel(config.channel());
      b.group(config.workerGroup());
      b.handler(new ChannelInitializer() {
        public void initChannel(Channel channel) {
          channel.pipeline()
            .addLast(clientSslCtx.newHandler(PooledByteBufAllocator.DEFAULT))
            .addLast(handler)
            .addLast(new HttpRequestEncoder())
          ;
        }
      });
      return b.connect(intermediaryDestination());
    }
  }
}
