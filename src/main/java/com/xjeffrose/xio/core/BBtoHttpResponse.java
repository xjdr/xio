package com.xjeffrose.xio.core;

import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.charset.Charset;
import java.util.Arrays;
import javax.annotation.Nullable;

public class BBtoHttpResponse {

  private BBtoHttpResponse() { }

  public static DefaultFullHttpResponse getResponse(ByteBuf byteBuf) {
    return BBtoHttpResponse.getResponse(null, byteBuf);
  }

  public static DefaultFullHttpResponse getResponse(@Nullable ChannelHandlerContext ctx, ByteBuf byteBuf) {
    // Lets make a HTTP parser cause apparently that's a good idea...
    final ByteBuf response = byteBuf.duplicate();
    final Joiner joiner = Joiner.on(" ").skipNulls();
    DefaultFullHttpResponse httpResponse;

    String[] headerBody = response.toString(Charset.defaultCharset()).split("\r\n\r\n");
    String[] headers = headerBody[0].split("\r\n");
    String[] firstLine = headers[0].split("\\s");

    // Lets make a HTTP Response object now
    if (ctx == null) {
      if (headerBody.length > 1) {
        httpResponse = new DefaultFullHttpResponse(
            HttpVersion.valueOf(firstLine[0]),
            new HttpResponseStatus(Integer.parseInt(firstLine[1]), joiner.join(Arrays.copyOfRange(firstLine, 2, 5))),
            Unpooled.wrappedBuffer(headerBody[1].getBytes()));
      } else {
        httpResponse = new DefaultFullHttpResponse(
            HttpVersion.valueOf(firstLine[0]),
            new HttpResponseStatus(Integer.parseInt(firstLine[1]), joiner.join(Arrays.copyOfRange(firstLine, 2, 5))));
      }
    } else {
      if (headerBody.length > 1) {
        httpResponse = new DefaultFullHttpResponse(
            HttpVersion.valueOf(firstLine[0]),
            new HttpResponseStatus(Integer.parseInt(firstLine[1]), joiner.join(Arrays.copyOfRange(firstLine, 2, 5))),
            ctx.alloc().buffer().writeBytes(headerBody[1].getBytes()));
      } else {
        httpResponse = new DefaultFullHttpResponse(
            HttpVersion.valueOf(firstLine[0]),
            new HttpResponseStatus(Integer.parseInt(firstLine[1]), joiner.join(Arrays.copyOfRange(firstLine, 2, 5))));
      }
    }

    for (int i = 1; i < headers.length; i++) {
      String[] xs = headers[i].split(":");
      httpResponse.headers().add(xs[0].trim(), xs[1].trim());
    }

    return httpResponse;
  }
}
