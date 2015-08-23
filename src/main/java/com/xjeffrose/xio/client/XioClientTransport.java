package com.xjeffrose.xio.client;

import io.airlift.units.Duration;
import java.io.Closeable;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;

class XioClientTransport extends SimpleChannelHandler implements Closeable {
  private final Channel channel;

  public XioClientTransport(Channel channel, Duration receiveTimeout) {
    this.channel = channel;
  }

  public void sendRequest() {
    HttpRequest request = new DefaultHttpRequest(
        HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    request.setHeader(HttpHeaders.Names.HOST, "www.google.com");
    request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
//    request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);

    // Send the HTTP request.
    channel.write(request);

    // Wait for the server to close the connection.
    channel.getCloseFuture().awaitUninterruptibly();
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
    HttpResponse response = (HttpResponse) e.getMessage();

    System.out.println("STATUS: " + response.getStatus());
    System.out.println("VERSION: " + response.getProtocolVersion());
    System.out.println();

    if (!response.getHeaderNames().isEmpty()) {
      for (String name : response.getHeaderNames()) {
        for (String value : response.getHeaders(name)) {
          System.out.println("HEADER: " + name + " = " + value);
        }
      }
      System.out.println();
    }

    ChannelBuffer content = response.getContent();

    System.out.println("CONTENT {");
    System.out.println(content.toString(CharsetUtil.UTF_8));
    System.out.println("} END OF CONTENT");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    //TODO: Prob log something?
    ctx.getChannel().close();
  }

  @Override
  public void close() {
    channel.close();
  }
}
