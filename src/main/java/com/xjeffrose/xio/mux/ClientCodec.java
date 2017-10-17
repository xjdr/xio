package com.xjeffrose.xio.mux;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.PromiseCombiner;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientCodec extends ChannelDuplexHandler {

  private static AttributeKey<Map<UUID, Request>> KEY = AttributeKey.newInstance("com.xjeffrose.xio.mux.RequestMap");

  private Object currentPayload;
  private boolean error = false;

  private void reset() {
    currentPayload = null;
    error = false;
  }

  private void setMapping(Channel channel, Map<UUID, Request> mapping) {
    channel.attr(KEY).set(mapping);
  }

  @VisibleForTesting
  Map<UUID, Request> getMapping(Channel channel) {
    return channel.attr(KEY).get();
  }

  @VisibleForTesting
  void setRequest(Map<UUID, Request> mapping, Request request) {
    mapping.put(request.getId(), request);
  }

  @VisibleForTesting
  Request getRequest(Map<UUID, Request> mapping, Message message) {
    return mapping.get(message.getId());
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    setMapping(ctx.channel(), Maps.newConcurrentMap());
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Message) {
      Message message = (Message)msg;
      if (currentPayload == null) {
        log.error("No response payload received for request id '{}': {}", message.getId(), message);
        ctx.fireExceptionCaught(new RuntimeException("No response payload received for request id '" + message.getId() + "'"));
        reset();
        return;
      }
      if (error) {
        log.error("Multiple response payloads received for request id '{}': {}", message.getId(), message);
        ctx.fireExceptionCaught(new RuntimeException("Multiple response payloads received for request id '" + message.getId() + "'"));
        reset();
        return;
      }
      Request request = getRequest(getMapping(ctx.channel()), message);
      if (request == null) {
        log.error("Unexpected response received for request id '{}': {}", message.getId(), message);
        ctx.fireExceptionCaught(new RuntimeException("Unexpected response received for request id '" + message.getId() + "'"));
        reset();
        return;
      }
      Response response = new Response(message.getId(), currentPayload);
      request.getResponsePromise().set(response);
      reset();
    } else {
      if (currentPayload != null) {
        error = true;
        return;
      }
      currentPayload = msg;
    }
  }

  private ChannelPromise write(ChannelHandlerContext ctx, Object msg) {
    ChannelPromise promise = ctx.newPromise();
    ctx.write(msg, promise);
    return promise;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof Message) {
      Message message = (Message)msg;
      Request request = message.getRequest();
      if (request.expectsResponse()) {
        setRequest(ctx.channel().attr(KEY).get(), request);
      }

      PromiseCombiner combiner = new PromiseCombiner();
      combiner.add(write(ctx, message.getPayload()));
      combiner.add(write(ctx, message));
      combiner.finish(promise);
    } else {
      throw new RuntimeException("Only Message objects can be written to ClientCodec");
    }
  }
}
