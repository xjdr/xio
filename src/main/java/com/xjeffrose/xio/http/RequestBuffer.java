package com.xjeffrose.xio.http;

import com.xjeffrose.xio.client.ClientState;
import com.xjeffrose.xio.client.DefaultChannelInitializer;
import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioClientBootstrap;
import com.xjeffrose.xio.server.Route;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import java.util.function.BiFunction;
import java.util.LinkedList;

@Slf4j
public class RequestBuffer extends ChannelDuplexHandler {

  // TODO(CK): look into using this event as well?
  // http://netty.io/4.1/api/io/netty/handler/codec/http2/Http2ConnectionPrefaceAndSettingsFrameWrittenEvent.html
  public static final class WriteReady {
    public static final WriteReady INSTANCE = new WriteReady();

    private WriteReady() {}
  }

  public static class ObjectAndPromise {
    public final Object object;
    public final ChannelPromise promise;

    ObjectAndPromise(Object object, ChannelPromise promise) {
      this.object = object;
      this.promise = promise;
    }

    public ChannelFuture apply(BiFunction<Object, ChannelPromise, ChannelFuture> write) {
      return write.apply(object, promise);
    }
  }

  private final List<ObjectAndPromise> writeBuffer = new LinkedList<>();

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (!(evt == WriteReady.INSTANCE)) {
      ctx.fireUserEventTriggered(evt);
      return;
    }

    // drain buffer
    while (writeBuffer.size() > 0) {
      ObjectAndPromise pair = writeBuffer.remove(0);

      /*
      if (writeBuffer.isEmpty()) {
        pair.apply(ctx::writeAndFlush);
      } else {
        pair.apply(ctx::write);
      }
      */

      pair.apply(writeBuffer.isEmpty() ? ctx::writeAndFlush : ctx::write)
          .addListener(
              new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) {
                  if (channelFuture.isSuccess()) {
                    log.debug("write finished for {}", pair.object);
                  } else {
                    log.error("Write error: ", channelFuture.cause());
                  }
                }
              });
    }
    // unhook this handler
    ctx.pipeline().remove(this);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    writeBuffer.add(new ObjectAndPromise(msg, promise));
  }

  @Override
  public void flush(ChannelHandlerContext ctx) {
    // no need to buffer the calls to flush, just ignore them
  }
}
