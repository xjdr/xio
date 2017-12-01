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
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2DataFrame;
import com.xjeffrose.xio.client.DefaultChannelInitializer;
import com.xjeffrose.xio.client.ClientState;
import io.netty.channel.Channel;
import java.util.List;
import java.util.ArrayList;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelFuture;

// TODO(CK): This class should be given a pool of clients to use.
//           The pool should be hidden behind a factory abstraction.
@Slf4j
public class Http2ProxyRoute implements Http2RouteProvider {

  private static final AttributeKey<XioClient> key = AttributeKey.newInstance("xio_h2_client");
  private final Route route;
  private final ProxyConfig config;
  private final XioClientBootstrap bootstrap;

  public Http2ProxyRoute(Route route, ProxyConfig config, XioClientBootstrap bootstrap) {
    this.config = config;
    this.route = route;
    this.bootstrap = bootstrap;
  }

  public static class H2Buffer extends ChannelDuplexHandler {
    // TODO(CK): Remove this hack after xio client is refactored
    List<Object> writeBuffer = new ArrayList<>();
    boolean active = false;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

      while (writeBuffer.size() > 0) {
        Object bufMessage = writeBuffer.remove(0);

        ctx.writeAndFlush(bufMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) {
              if (channelFuture.isSuccess()) {
                log.debug("write finished for " + bufMessage);
                //newPromise.setSuccess(null);
              } else {
                log.error("Write error: ", channelFuture.cause());
                //newPromise.setFailure(channelFuture.cause());
              }
            }
          });
      }

      active = true;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if (active) {
        ctx.writeAndFlush(msg, promise);
      } else {
        writeBuffer.add(msg);
      }
    }

  }

  public static class ProxyInitializer extends DefaultChannelInitializer {
    public ProxyInitializer(ClientState state) {
      super(state);
    }

    @Override
    public void initChannel(Channel channel) {
      super.initChannel(channel);
      channel.pipeline()
        .addAfter("protocol handler", "stream mapper", new Http2StreamMapper())
        .addLast("h2 buffer", new H2Buffer());
        ;

    }
  }

  private XioClient buildClient(ChannelHandlerContext ctx) {
    XioClient client = ctx.channel().attr(key).get();
    if (client == null) {
      client = bootstrap.clone(ctx.channel().eventLoop())
        .address(config.address)
        .ssl(config.needSSL)
        .initializerFactory(ProxyInitializer::new)
        .applicationProtocol(() -> new Http2HandlerBuilder(Http2FrameForwarder::create).server(false).build())
        .handler(new RawBackendHandler(ctx))
        .build()
        ;
      ctx.channel().attr(key).set(client);
    }
    return client;
  }

  private static XioClient getClient(ChannelHandlerContext ctx) {
    XioClient client = ctx.channel().attr(key).get();
    if (client == null) {
      throw new RuntimeException("Coudln't find the xio client for Http2RouteProvider.getClient(" + ctx + ")");
    }
    return client;
  }

  void handleHeaders(ChannelHandlerContext ctx, Http2Request request) {
    log.debug("handleHeaders: {} {}", ctx, request);
    XioClient client = buildClient(ctx);


    /*
    // TODO(CK): How do we trace over http2?
    XioRequest request;

    if (HttpTracingState.hasSpan(ctx)) {
      request = new XioRequest(payload, HttpTracingState.getSpan(ctx).context());
    } else {
      request = new XioRequest(payload, null);
    }
    */

    log.info("Requesting {}", request);
    client.write(request);
  }

  void handleData(ChannelHandlerContext ctx, Http2Request request) {
    log.debug("handleData: {} {}", ctx, request);
    XioClient client = getClient(ctx);
    client.write(request);
  }

  @Override
  public void handle(Http2Request request, ChannelHandlerContext ctx) {
    if (request.payload instanceof Http2Headers) {
      handleHeaders(ctx, request);
    } else if (request.payload instanceof Http2DataFrame) {
      Http2DataFrame data = (Http2DataFrame)request.payload;
      ReferenceCountUtil.retain(data);
      handleData(ctx, request);
    }
  }

  @Override
  public void close(ChannelHandlerContext ctx) {
    XioClient client = ctx.channel().attr(key).get();
    if (client != null) {
      try {
        client.close();
      } catch(java.io.IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
