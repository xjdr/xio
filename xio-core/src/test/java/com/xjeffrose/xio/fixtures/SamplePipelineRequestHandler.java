package com.xjeffrose.xio.fixtures;

import com.xjeffrose.xio.http.FullRequest;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.PipelineRequestHandler;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Response;
import com.xjeffrose.xio.http.ResponseBuilders;
import com.xjeffrose.xio.http.RouteState;
import com.xjeffrose.xio.http.StreamingData;
import com.xjeffrose.xio.http.StreamingRequestData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SamplePipelineRequestHandler implements PipelineRequestHandler {

  private final StringBuilder buf = new StringBuilder();
  /** Buffer that stores the response content */
  private HttpRequest request;

  private ByteBuf body() {
    return Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8);
  }

  public void handle(ChannelHandlerContext ctx, Request request, RouteState route) {
    buf.setLength(0);
    buf.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
    buf.append("===================================\r\n");

    buf.append("VERSION: ").append(request.version()).append("\r\n");
    buf.append("HOSTNAME: ").append(request.host("unknown")).append("\r\n");
    buf.append("REQUEST_URI: ").append(request.path()).append("\r\n\r\n");

    Headers headers = request.headers();
    if (!headers.isEmpty()) {
      for (Map.Entry<CharSequence, CharSequence> h : headers) {
        CharSequence key = h.getKey();
        CharSequence value = h.getValue();
        buf.append("HEADER: ").append(key).append(" = ").append(value).append("\r\n");
      }
      buf.append("\r\n");
    }

    QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.path());
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

    Response response = null;
    if (request instanceof FullRequest) {
      FullRequest fullRequest = (FullRequest) request;

      ByteBuf content = fullRequest.body();
      if (content.isReadable()) {
        buf.append("CONTENT: ");
        buf.append(content.toString(CharsetUtil.UTF_8));
        buf.append("\r\n");
      }
      response = ResponseBuilders.newOk(request).body(this.body()).build();
    }

    if (request instanceof StreamingRequestData) {
      StreamingData data = (StreamingData) request;
      if (data.endOfStream()) {
        buf.append("END OF CONTENT\r\n");

        if (!data.trailingHeaders().isEmpty()) {
          buf.append("\r\n");
          for (CharSequence name : data.trailingHeaders().names()) {
            for (CharSequence value : data.trailingHeaders().getAll(name)) {
              buf.append("TRAILING HEADER: ");
              buf.append(name).append(" = ").append(value).append("\r\n");
            }
          }
          buf.append("\r\n");
        }
        response = ResponseBuilders.newOk(request).body(this.body()).build();
      }
    }

    if (response != null) {
      ctx.writeAndFlush(response);
    }
  }
}
