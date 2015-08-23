package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.ListenableFuture;
import java.net.URI;
import org.jboss.netty.channel.Channel;
import org.junit.Test;

import static org.junit.Assert.*;

public class XioClientTest {

  @Test
  public void testConnect() throws Exception {
    HttpClientConnector httpClientConnector = new HttpClientConnector(URI.create("http://www.google.com"));
    XioClient xioClient = new XioClient();
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(httpClientConnector);
    XioClientChannel xioClientChannel = responseFuture.get();
  }

  @Test
  public void testConnectAsync() throws Exception {

  }
}