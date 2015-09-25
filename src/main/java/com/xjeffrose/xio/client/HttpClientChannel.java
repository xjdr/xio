package com.xjeffrose.xio.client;


import com.google.common.net.HttpHeaders;
import com.xjeffrose.xio.core.XioException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Timer;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.log4j.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Values.CLOSE;
import static io.netty.handler.codec.http.HttpHeaders.Values.GZIP;


@NotThreadSafe
public class HttpClientChannel extends AbstractClientChannel {
  private static final Logger log = Logger.getLogger(HttpClientChannel.class.getName());

  private final Channel underlyingNettyChannel;
  private final Timer timer;
  private final XioProtocolFactory protocolFactory;
  private final URI uri;

  private Map<String, String> headerDictionary;

  protected HttpClientChannel(Channel channel,
                              Timer timer,
                              XioProtocolFactory protocolFactory,
                              URI uri) {
    super(channel, timer, protocolFactory);

    this.underlyingNettyChannel = channel;
    this.timer = timer;
    this.protocolFactory = protocolFactory;
    this.uri = uri;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headerDictionary = headers;
  }

  @Override
  protected ByteBuf extractResponse(Object message) throws XioException {
    if (!(message instanceof HttpObject)) {
      return null;
    }

    HttpResponse httpResponse = null;
    HttpContent httpContent = null;
    ByteBuf content = null;

    XioClientChannel xioClientChannel;

    if (message instanceof HttpResponse) {
      httpResponse = (HttpResponse) message;
    }

    if (message instanceof HttpContent) {
      httpContent = (HttpContent) message;

      if (httpContent.getDecoderResult() == DecoderResult.SUCCESS) {
        content = httpContent.content();
      }

      if (content != null) {
        if (!content.isReadable()) {
          return null;
        }
      }
    }

    //TODO(JR): Leave out for testing, ADD BACK BEFORE DEPLOYMENT!!!!!
//      switch (httpResponse.getStatus().reasonPhrase()) {
//        case("Unknown Status"):
//          throw wrapException(new XioTransportException("HTTP response had non-OK status: " + httpResponse
//            .getStatus().toString()));
//        case("Informational"):
//          break;
//        case("Successful"):
//          break;
//        case("Redirection"):
//          break;
//        case("Client Error"):
//          throw wrapException(new XioTransportException("HTTP response had non-OK status: " + httpResponse
//              .getStatus().toString()));
//      }

//      HttpContent httpContent = (HttpContent) httpResponse;


    String CRLF = "\r\n";
    StringBuilder responseHeader = new StringBuilder();
    responseHeader
        .append(httpResponse.getProtocolVersion())
        .append(' ')
        .append(httpResponse.getStatus())
        .append(CRLF);

    httpResponse.headers().entries().forEach(xs -> {
      responseHeader
          .append(xs.getKey())
          .append(": ")
          .append(xs.getValue())
          .append(CRLF);
    });

    responseHeader.append(CRLF);

    ByteBuf headerAndBody = getCtx().alloc().buffer();
    headerAndBody.writeBytes(responseHeader.toString().getBytes(Charset.defaultCharset()));
    headerAndBody.writeBytes(content);
    headerAndBody.writeBytes("\r\n".getBytes());
    return headerAndBody;
  }

  @Override
  protected ChannelFuture writeRequest(@Nullable ByteBuf request) {
    DefaultFullHttpRequest httpRequest;

    if (request == Unpooled.EMPTY_BUFFER || request == null) {
      httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getPath());
      httpRequest.headers().add(HttpHeaders.CONTENT_LENGTH, "0");
    } else {
      httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri.getPath(), request);
      httpRequest.headers().add(HttpHeaders.CONTENT_LENGTH, request.readableBytes());
    }

    if (headerDictionary != null) {
      for (Map.Entry<String, String> entry : headerDictionary.entrySet()) {
        httpRequest.headers().add(entry.getKey(), entry.getValue());
      }
    }

    httpRequest.headers().set(HttpHeaders.CONNECTION, CLOSE);
//    httpRequest.headers().set(HttpHeaders.ACCEPT_ENCODING, GZIP);

    log.debug("HTTP Request from XIO:\n" + httpRequest);

    return underlyingNettyChannel.writeAndFlush(httpRequest);
  }
}