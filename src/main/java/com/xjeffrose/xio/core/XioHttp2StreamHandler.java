package com.xjeffrose.xio.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.CharsetUtil;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;

public class XioHttp2StreamHandler extends ChannelDuplexHandler {

  static final ByteBuf RESPONSE_BYTES = unreleasableBuffer(copiedBuffer("Hello World", CharsetUtil.UTF_8));

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
    cause.printStackTrace();
    ctx.close();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Http2HeadersFrame) {
      onHeadersRead(ctx, (Http2HeadersFrame) msg);
    } else if (msg instanceof Http2DataFrame) {
      onDataRead(ctx, (Http2DataFrame) msg);
    } else {
      super.channelRead(ctx, msg);
    }
  }

  /**
   * If receive a frame with end-of-stream set, send a pre-canned response.
   */
  public void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data) throws Exception {
    if (data.isEndStream()) {
      sendResponse(ctx, data.content().retain());
    }
  }

  /**
   * If receive a frame with end-of-stream set, send a pre-canned response.
   */
  public void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame headers)
      throws Exception {
    if (headers.isEndStream()) {
      ByteBuf content = ctx.alloc().buffer();
      content.writeBytes(RESPONSE_BYTES);
      ByteBufUtil.writeAscii(content, " - via HTTP/2");
      sendResponse(ctx, content);
    }
  }

  /**
   * Sends a "Hello World" DATA frame to the client.
   */
  private void sendResponse(ChannelHandlerContext ctx, ByteBuf payload) {
    // Send a frame for the response status
    Http2Headers headers = new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText());
    ctx.write(new DefaultHttp2HeadersFrame(headers));
    ctx.writeAndFlush(new DefaultHttp2DataFrame(payload, true));
  }
}


