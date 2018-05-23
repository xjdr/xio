package com.xjeffrose.xio.backend.server;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

@Slf4j
public class ChannelRequestInboundHandler extends ChannelInboundHandlerAdapter {

  private final ImmutableMap<String, RequestHandler> handlers;

  ChannelRequestInboundHandler(ImmutableMap<String, RequestHandler> handlers) {
    this.handlers = handlers;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    log.debug("read {}", msg);
    if (msg instanceof FullHttpRequest) {
      FullHttpRequest request = (FullHttpRequest) msg;
      String path = request.uri();
      RequestHandler handler = handlers.get(path);
      if (handler != null) {
        ResponseBuilder responseBuilder = new ResponseBuilder(ctx.alloc());
        CharSequence echo = request.headers().get("x-echo", "none");
        CharSequence method = request.method().asciiName();
        responseBuilder = handler.request(responseBuilder).addEcho(echo);
        FullHttpResponse response = handler.request(responseBuilder)
          .addEcho(echo)
          .addMethodEcho(method)
          .buildH1();
        log.debug("write flushing response {}", response);
        ctx.writeAndFlush(response);
        return;
      } else {
        FullHttpResponse ruhRoh = new ResponseBuilder(ctx.alloc())
          .setStatus(HttpResponseStatus.NOT_FOUND)
          .addHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setBody(new PojoResponse("ruh roh", "not found!"))
          .buildH1();
        log.debug("write flushing {}", ruhRoh);
        ctx.writeAndFlush(ruhRoh);
        return;
      }
    }
    log.error("not sending response!");
    ctx.fireChannelRead(msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("error: {}", cause);
    String message = cause.getMessage();
    final ByteBuf data;
    if (message == null) {
      data = Unpooled.EMPTY_BUFFER;
    } else {
      data = copiedBuffer(message.getBytes());
    }
    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
      HttpResponseStatus.INTERNAL_SERVER_ERROR, data);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.readableBytes());
    ctx.writeAndFlush(response);
  }
}
