package com.xjeffrose.xio.helpers;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.client.ChannelConfiguration;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class HttpProxyServer extends ProxyServer {

  public HttpProxyServer(InetSocketAddress destination) {
    super(destination);
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
            .addLast(handler)
            .addLast(new HttpRequestEncoder())
          ;
        }
      });
      return b.connect(intermediaryDestination());
    }
  }
}
