package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.ListenableFuture;
import java.lang.Thread;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import org.eclipse.jetty.http.HttpStatus;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Test;
import sun.jvm.hotspot.runtime.*;

import static org.junit.Assert.*;

public class XioClientTest {

  @Test
  public void testConnect() throws Exception {
    XioClient xioClient = new XioClient();
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("http://www.google.com/")));
      XioClientChannel xioClientChannel = responseFuture.get();
    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

    RequestChannel.Listener listener = new RequestChannel.Listener() {
      private ChannelBuffer xxx;

      @Override
      public void onRequestSent() {
        System.out.println("Request Sent");
      }

      @Override
      public void onResponseReceived(ChannelBuffer message) {
        System.out.println("Response Recieved");
//        System.out.println(message.toString(Charset.defaultCharset()));
        xxx = message;
      }

      @Override
      public void onChannelError(XioException requestException) {
        System.out.println("Request Error");
        requestException.printStackTrace();
      }

      @Override
      public ChannelBuffer getResponse() {
        return xxx;
      }
    };

    httpClientChannel.sendAsynchronousRequest(ChannelBuffers.EMPTY_BUFFER, false, listener);

    Thread.sleep(1000);

    System.out.println("Content: " + listener.getResponse().toString(Charset.defaultCharset()));

  }
}