package com.xjeffrose.xio.core;

import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.charset.Charset;
import java.util.Arrays;
import javax.annotation.Nullable;

public class BBtoHttpResponse {

  private BBtoHttpResponse() {}

  public static DefaultFullHttpResponse getResponse(ByteBuf byteBuf) {
    return BBtoHttpResponse.getResponse(null, byteBuf);
  }

  public static DefaultFullHttpResponse getResponse(
      @Nullable ChannelHandlerContext ctx, ByteBuf byteBuf) {
    // Lets make a HTTP parser cause apparently that's a good idea...
    final ByteBuf response = byteBuf.duplicate();
    final Joiner joiner = Joiner.on(" ").skipNulls();
    DefaultFullHttpResponse httpResponse;

    String[] headers = null;
    ByteBuf body = null;
    String delimiter = "\r\n\r\n";
    ByteBufProcessor processor = new MultiBytesFieldSplitProcessor(delimiter.getBytes());
    int delimIdx = response.forEachByte(0, response.readableBytes(), processor);
    if (delimIdx < 0) {
      headers = response.toString(Charset.defaultCharset()).split("\r\n");
      body = null;
    } else {
      ByteBuf h = response.slice(0, delimIdx - delimiter.length() + 1);
      headers = h.toString(Charset.defaultCharset()).split("\r\n");
      delimIdx++; // move to start of body
      body = response.slice(delimIdx, byteBuf.readableBytes() - delimIdx);
    }

    String[] firstLine = headers[0].split("\\s");

    // Lets make a HTTP Response object now
    if (ctx == null) {
      if (body != null) {
        httpResponse =
            new DefaultFullHttpResponse(
                HttpVersion.valueOf(firstLine[0]),
                new HttpResponseStatus(
                    Integer.parseInt(firstLine[1]),
                    joiner.join(Arrays.copyOfRange(firstLine, 2, 5))),
                Unpooled.wrappedBuffer(body));
      } else {
        httpResponse =
            new DefaultFullHttpResponse(
                HttpVersion.valueOf(firstLine[0]),
                new HttpResponseStatus(
                    Integer.parseInt(firstLine[1]),
                    joiner.join(Arrays.copyOfRange(firstLine, 2, 5))));
      }
    } else {
      if (body != null) {
        httpResponse =
            new DefaultFullHttpResponse(
                HttpVersion.valueOf(firstLine[0]),
                new HttpResponseStatus(
                    Integer.parseInt(firstLine[1]),
                    joiner.join(Arrays.copyOfRange(firstLine, 2, 5))),
                ctx.alloc().buffer().writeBytes(body));
      } else {
        httpResponse =
            new DefaultFullHttpResponse(
                HttpVersion.valueOf(firstLine[0]),
                new HttpResponseStatus(
                    Integer.parseInt(firstLine[1]),
                    joiner.join(Arrays.copyOfRange(firstLine, 2, 5))));
      }
    }

    for (int i = 1; i < headers.length; i++) {
      String[] xs = headers[i].split(":", 2);
      httpResponse.headers().add(xs[0].trim(), xs[1].trim());
    }

    return httpResponse;
  }

  static class MultiBytesFieldSplitProcessor implements ByteBufProcessor {

    private final byte[] delimiter;
    private int index;

    public MultiBytesFieldSplitProcessor(byte[] recordDelimiterByte) {
      this.delimiter = recordDelimiterByte;
    }

    @Override
    public boolean process(byte value) throws Exception {
      if (delimiter[index] != value) {
        index = 0;
        return true;
      }
      if (index != delimiter.length - 1) {
        index++;
        return true;
      }
      index = 0;
      return false;
    }
  }
}
