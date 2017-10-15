package com.xjeffrose.xio.fixtures;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SampleHandler extends SimpleChannelInboundHandler<Object> {

  /** Buffer that stores the response content */
  private final StringBuilder buf = new StringBuilder();
  private HttpRequest request;

  private static void appendDecoderResult(StringBuilder buf, HttpObject o) {
    DecoderResult result = o.getDecoderResult();
    if (result.isSuccess()) {
      return;
    }

    buf.append(".. WITH DECODER FAILURE: ");
    buf.append(result.cause());
    buf.append("\r\n");
  }

  private static void send100Continue(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
    ctx.write(response);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest) {
      HttpRequest request = this.request = (HttpRequest) msg;

      if (HttpHeaders.is100ContinueExpected(request)) {
        send100Continue(ctx);
      }

      buf.setLength(0);
      buf.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
      buf.append("===================================\r\n");

      buf.append("VERSION: ").append(request.getProtocolVersion()).append("\r\n");
      buf.append("HOSTNAME: ").append(HttpHeaders.getHost(request, "unknown")).append("\r\n");
      buf.append("REQUEST_URI: ").append(request.getUri()).append("\r\n\r\n");

      HttpHeaders headers = request.headers();
      if (!headers.isEmpty()) {
        for (Map.Entry<String, String> h : headers) {
          String key = h.getKey();
          String value = h.getValue();
          buf.append("HEADER: ").append(key).append(" = ").append(value).append("\r\n");
        }
        buf.append("\r\n");
      }

      QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
      Map<String, List<String>> params = queryStringDecoder.parameters();
      if (!params.isEmpty()) {
        for (Entry<String, List<String>> p : params.entrySet()) {
          String key = p.getKey();
          List<String> vals = p.getValue();
          for (String val : vals) {
            buf.append("PARAM: ").append(key).append(" = ").append(val).append("\r\n");
          }
        }
        buf.append("\r\n");
      }

      appendDecoderResult(buf, request);
    }

    if (msg instanceof HttpContent) {
      HttpContent httpContent = (HttpContent) msg;

      ByteBuf content = httpContent.content();
      if (content.isReadable()) {
        buf.append("CONTENT: ");
        buf.append(content.toString(CharsetUtil.UTF_8));
        buf.append("\r\n");
        appendDecoderResult(buf, request);
      }

      if (msg instanceof LastHttpContent) {
        buf.append("END OF CONTENT\r\n");

        LastHttpContent trailer = (LastHttpContent) msg;
        if (!trailer.trailingHeaders().isEmpty()) {
          buf.append("\r\n");
          for (String name : trailer.trailingHeaders().names()) {
            for (String value : trailer.trailingHeaders().getAll(name)) {
              buf.append("TRAILING HEADER: ");
              buf.append(name).append(" = ").append(value).append("\r\n");
            }
          }
          buf.append("\r\n");
        }

        if (!writeResponse(trailer, ctx)) {
          // If keep-alive is off, close the connection once the content is fully written.
          ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
      }
    }
  }

  private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
    // Decide whether to close the connection or not.
    boolean keepAlive = HttpHeaders.isKeepAlive(request);
    // Build the response object.
    FullHttpResponse response = new DefaultFullHttpResponse(
        HTTP_1_1, currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
        Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

    if (keepAlive) {
      // Add 'Content-Length' header only for a keep-alive connection.
      response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
      // Add keep alive header as per:
      // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    // Encode the cookie.
    String cookieString = request.headers().get(COOKIE);
    if (cookieString != null) {
      Set<Cookie> cookies = CookieDecoder.decode(cookieString);
      if (!cookies.isEmpty()) {
        // Reset the cookies if necessary.
        for (Cookie cookie : cookies) {
          response.headers().add(SET_COOKIE, ServerCookieEncoder.encode(cookie));
        }
      }
    } else {
      // Browser sent no cookie.  Add some.
      response.headers().add(SET_COOKIE, ServerCookieEncoder.encode("key1", "value1"));
      response.headers().add(SET_COOKIE, ServerCookieEncoder.encode("key2", "value2"));
    }

    // Write the response.
    ctx.write(response);

    return keepAlive;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}