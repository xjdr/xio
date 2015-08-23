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
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("http://www.paypal.com")));
      XioClientChannel xioClientChannel = responseFuture.get();
    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

    RequestChannel.Listener listener = new RequestChannel.Listener() {
      private ChannelBuffer response;

      @Override
      public void onRequestSent() {
        System.out.println("Request Sent");
      }

      @Override
      public void onResponseReceived(ChannelBuffer message) {
        System.out.println("Response Recieved");
        System.out.println(message.toString(Charset.defaultCharset()));
        response = message;
      }

      @Override
      public void onChannelError(XioException requestException) {
        System.out.println("Request Error");
        requestException.printStackTrace();
      }

      @Override
      public ChannelBuffer getResponse() {
        return response;
      }
    };

    httpClientChannel.sendAsynchronousRequest(ChannelBuffers.EMPTY_BUFFER, false, listener);

    Thread.sleep(1000);

//    System.out.println(listener.getResponse().toString(Charset.defaultCharset()));


  }
//
//  @Test
//  public void testConnectSync() throws Exception {
//    XioClient xioClient = new XioClient();
//    XioClientTransport clientTransport = xioClient.connectSync(new InetSocketAddress("google.com", 80));
//  }
//
//  @Test
//  public void testConnectAsync() throws Exception {
//    XioClient xioClient = new XioClient();
//    XioClientTransport clientTransport = xioClient.connectSync(new HttpClientConnector(URI.create("http://www.google.com/")));
//  }
}