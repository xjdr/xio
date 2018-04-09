package com.xjeffrose.xio.mux;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.PromiseCombiner;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerCodec extends ChannelDuplexHandler {

  private Object currentPayload;
  private boolean error = false;

  private void reset() {
    currentPayload = null;
    error = false;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Message) {
      Message message = (Message) msg;
      if (currentPayload == null) {
        log.error("No payload received for request id '{}': {}", message.getId(), message);
        ctx.fireExceptionCaught(
            new RuntimeException("No payload received for request id '" + message.getId() + "'"));
        reset();
        return;
      }
      if (error) {
        log.error("Multiple payloads received for request id '{}': {}", message.getId(), message);
        ctx.fireExceptionCaught(
            new RuntimeException(
                "Multiple payloads received for request id '" + message.getId() + "'"));
        reset();
        return;
      }
      ServerRequest request =
          new ServerRequest(message.getId(), message.expectsResponse(), currentPayload);
      ctx.fireChannelRead(request);
      reset();
    } else {
      if (currentPayload != null) {
        error = true;
        return;
      }
      if (msg instanceof ReferenceCounted) {
        currentPayload = ((ReferenceCounted) msg).retain();
      } else {
        currentPayload = msg;
      }
    }
  }

  private ChannelPromise write(ChannelHandlerContext ctx, Object msg) {
    ChannelPromise promise = ctx.newPromise();
    ctx.write(msg, promise);
    return promise;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof Response) {
      Response response = (Response) msg;

      PromiseCombiner combiner = new PromiseCombiner();
      combiner.add(write(ctx, response.getPayload()));
      combiner.add(write(ctx, Message.buildResponse(response.getInResponseTo())));
      combiner.finish(promise);
    } else {
      throw new RuntimeException("Only Message objects can be written to ServerCodec");
    }
  }
}
