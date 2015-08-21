package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.ListenableFuture;
import java.net.URI;
import org.jboss.netty.channel.Channel;
import org.junit.Test;

import static org.junit.Assert.*;

public class XioClientTest {

  @Test
  public void testConnect() throws Exception {
    XioClient xioClient = new XioClient();
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connect(new URI("https://google.com"));
  }

  @Test
  public void testConnectAsync() throws Exception {

  }
}