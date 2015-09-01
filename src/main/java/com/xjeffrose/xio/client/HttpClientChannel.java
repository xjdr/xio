package com.xjeffrose.xio.client;


import com.google.common.net.HttpHeaders;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Timer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class HttpClientChannel extends AbstractClientChannel {
  private final Channel underlyingNettyChannel;
  private final String hostName;
  private final String endpointUri;
  private Map<String, String> headerDictionary;

  protected HttpClientChannel(Channel channel,
                              Timer timer,
                              XioProtocolFactory protocolFactory,
                              String hostName,
                              String endpointUri) {
    super(channel, timer, protocolFactory);

    this.underlyingNettyChannel = channel;
    this.hostName = hostName;
    this.endpointUri = endpointUri;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headerDictionary = headers;
  }

  public ByteBuf sendProxyRequest(final ByteBuf message) throws XioException {
    final ByteBuf[] response = new ByteBuf[1];
    final Lock lock = new ReentrantLock();
    final Condition waitForFinish = lock.newCondition();

    Listener listener = new Listener() {
      @Override
      public void onRequestSent() {

      }

      @Override
      public void onResponseReceived(ByteBuf message) {
        response[0] = message;
        message.retain();

        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public void onChannelError(XioException requestException) {

      }
    };

    sendAsynchronousRequest(message, false, listener);

    try {
      lock.lock();
      waitForFinish.await();
      lock.unlock();

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return response[0];
  }

  @Override
  protected ByteBuf extractResponse(Object message) throws XioTransportException {
    if (!(message instanceof HttpObject)) {
      return null;
    }

    if (message instanceof HttpResponse) {

      HttpResponse httpResponse = (HttpResponse) message;

      if (!httpResponse.getStatus().equals(HttpResponseStatus.OK)) {
        throw new XioTransportException("HTTP response had non-OK status: " + httpResponse
            .getStatus().toString());
      }

      HttpContent httpContent = (HttpContent) httpResponse;
      ByteBuf content = httpContent.content();
      content.retain();

      if (!content.isReadable()) {
        return null;
      }

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
      content.retain();
      headerAndBody.writeBytes(content);
      return headerAndBody;
    }
    return null;
  }

  @Override
  protected ChannelFuture writeRequest(ByteBuf request) {
    HttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, endpointUri, request);

    httpRequest.headers().add(HttpHeaders.HOST, hostName);
    httpRequest.headers().add(HttpHeaders.CONTENT_LENGTH, request.readableBytes());
    httpRequest.headers().add(HttpHeaders.CONTENT_TYPE, "application/text");
//    httpRequest.headers().add(HttpHeaders.ACCEPT_ENCODING, "gzip");
    httpRequest.headers().add(HttpHeaders.USER_AGENT, "xio/0.4.0");

    if (headerDictionary != null) {
      for (Map.Entry<String, String> entry : headerDictionary.entrySet()) {
        httpRequest.headers().add(entry.getKey(), entry.getValue());
      }
    }

    return underlyingNettyChannel.writeAndFlush(httpRequest);
  }

}