package com.xjeffrose.xio.fixtures;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.xjeffrose.xio.processor.XioProcessor;
import com.xjeffrose.xio.server.RequestContext;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class XioTestProcessor implements XioProcessor {

  @Override
  public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, Object msg, RequestContext respCtx) {
    ListeningExecutorService service = MoreExecutors.listeningDecorator(ctx.executor());

    ListenableFuture<Boolean> httpResponseFuture = service.submit(new Callable<Boolean>() {
      public Boolean call() {
        HttpMessage httpMessage = null;
        HttpRequest request = null;

        if (msg instanceof HttpMessage) {
          httpMessage = (HttpMessage) msg;
          request = (HttpRequest) httpMessage;
        }

        final StringBuilder buf = new StringBuilder();

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

        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
            HTTP_1_1, request.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
            Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

        if (keepAlive) {
          // Add keep alive header as per:
          // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
          response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        if (msg instanceof LastHttpContent) {
          buf.append("END OF CONTENT\r\n");
        }

        respCtx.setContextData(respCtx.getConnectionId(), response);

        return true;
      }
    });
    return httpResponseFuture;
  }
}
