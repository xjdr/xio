package com.xjeffrose.xio.client;


import com.google.common.net.HttpHeaders;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.Timer;

@NotThreadSafe
public class HttpClientChannel extends AbstractClientChannel implements XioClientChannel {
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

  @Override
  public Channel getNettyChannel() {
    return underlyingNettyChannel;
  }

  @Override
  protected ChannelBuffer extractResponse(Object message) throws XioTransportException {
    if (!(message instanceof HttpResponse)) {
      return null;
    }

    HttpResponse httpResponse = (HttpResponse) message;

    if (!httpResponse.getStatus().equals(HttpResponseStatus.OK)) {
      throw new XioTransportException("HTTP response had non-OK status: " + httpResponse
          .getStatus().toString());
    }

    ChannelBuffer content = httpResponse.getContent();

    if (!content.readable()) {
      return null;
    }

    return content;
  }

  @Override
  protected ChannelFuture writeRequest(ChannelBuffer request) {
    HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
        endpointUri);

    httpRequest.setHeader(HttpHeaders.HOST, hostName);
    httpRequest.setHeader(HttpHeaders.CONTENT_LENGTH, request.readableBytes());
    httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-thrift");
    httpRequest.setHeader(HttpHeaders.ACCEPT, "application/x-thrift");
    httpRequest.setHeader(HttpHeaders.USER_AGENT, "Java/Swift-HttpThriftClientChannel");

    if (headerDictionary != null) {
      for (Map.Entry<String, String> entry : headerDictionary.entrySet()) {
        httpRequest.setHeader(entry.getKey(), entry.getValue());
      }
    }

    httpRequest.setContent(request);

    return underlyingNettyChannel.write(httpRequest);
  }

  public void setHeaders(Map<String, String> headers) {
    this.headerDictionary = headers;
  }

}