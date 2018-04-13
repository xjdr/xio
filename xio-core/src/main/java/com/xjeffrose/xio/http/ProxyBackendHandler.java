package com.xjeffrose.xio.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyBackendHandler extends ChannelDuplexHandler {

  private static AttributeKey<Integer> CHANNEL_STREAM_ID_KEY =
      AttributeKey.newInstance("xio_channel_stream_id");

  private static void setChannelStreamId(ChannelHandlerContext ctx, int streamId) {
    ctx.channel().attr(CHANNEL_STREAM_ID_KEY).set(streamId);
  }

  @Nullable
  private static Integer getChannelStreamId(ChannelHandlerContext ctx) {
    return ctx.channel().attr(CHANNEL_STREAM_ID_KEY).get();
  }

  private final ChannelHandlerContext frontend;
  private boolean needFlush = false;

  public ProxyBackendHandler(ChannelHandlerContext frontend) {
    this.frontend = frontend;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    if (ctx.pipeline().get(Http2Handler.class) != null) {
      log.debug("handlerAdded: adding Http2StreamMapper");
      // we are an http2 pipeline
      ctx.pipeline().addBefore("application codec", "stream mapper", new Http2StreamMapper());
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof Request) {
      setChannelStreamId(ctx, ((Request) msg).streamId());
    }
    ctx.write(msg, promise);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    log.debug("RawBackendHandler[{}] channelRead: {}", this, msg);
    if (msg instanceof Response) {
      msg = responseWithPreservedStreamId(ctx, (Response) msg);
    }
    frontend
        .write(msg)
        .addListener(
            (ChannelFutureListener)
                f -> {
                  if (f.cause() != null) {
                    // TODO(CK): move this into a logger class
                    log.error("Write Error!", f.cause());
                  }
                });
    needFlush = true;
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    log.debug("RawBackendHandler[{}] channelReadComplete", this);
    if (needFlush) {
      frontend.flush();
      needFlush = false;
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    log.debug("RawBackendHandler[{}] channelInactive", this);
    // TODO(CK): this should really be some sort of notification to the frontend
    // that the backend closed. Keepalive/h2 will require the connection to stay open, we
    // shouldn't be closing it.
    frontend.close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.debug("RawBackendHandler[{}] exceptionCaught: {}", this, cause);
    ctx.close();
  }

  @Nullable
  public Response responseWithPreservedStreamId(ChannelHandlerContext ctx, Response response) {
    Integer streamId = getChannelStreamId(ctx);
    if (streamId != null && streamId != Message.H1_STREAM_ID_NONE) {
      if (response instanceof FullResponse) {
        return DefaultFullResponse.from((FullResponse) response).streamId(streamId).build();
      } else if (response instanceof SegmentedResponse) {
        return DefaultSegmentedResponse.from((SegmentedResponse) response)
            .streamId(streamId)
            .build();
      }
    }
    return response;
  }
}
