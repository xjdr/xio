package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.handler.codec.http.CombinedHttpHeaders;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpsUpgradeHandler informs clients that they must upgrade their
 * connection to SSL then closes the connection.
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpsUpgradeHandler extends ChannelOutboundHandlerAdapter {

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    List<ByteBuf> payload;

    HttpHeaders headers = new CombinedHttpHeaders(true);
    headers.add(HttpHeaderNames.UPGRADE, "TLS/1.2");
    headers.add(HttpHeaderNames.UPGRADE, HTTP_1_1);
    headers.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
    headers.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    headers.add(HttpHeaderNames.CONTENT_LENGTH, "0");
    DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                                                        HTTP_1_1,
                                                        UPGRADE_REQUIRED,
                                                        Unpooled.EMPTY_BUFFER,
                                                        headers,
                                                        EmptyHttpHeaders.INSTANCE
                                                        );
    payload = Recipes.encodeResponse(response);

    for (ByteBuf buffer : payload) {
      ctx.write(buffer.copy());
    }
    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
  }
}
