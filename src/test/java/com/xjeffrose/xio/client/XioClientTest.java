package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.SSL.SSLEngineFactory;
import com.xjeffrose.xio.core.XioSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.net.ssl.SSLEngine;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.handler.ssl.SslHandler;
import org.junit.Test;

import static org.junit.Assert.*;

public class XioClientTest {

  @Test
  public void testConnectHttp() throws Exception {

    XioClient xioClient = new XioClient();
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("http://www.google.com/")));
      XioClientChannel xioClientChannel = responseFuture.get();
    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

   Listener listener = new Listener() {
      @Override
      public void onRequestSent() {
        System.out.println("Request Sent");
      }

      @Override
      public void onResponseReceived(ChannelBuffer message) {
        System.out.println("Response Recieved");
        System.out.println(message.toString(Charset.defaultCharset()));
      }

      @Override
      public void onChannelError(XioException requestException) {
        System.out.println("Request Error");
        requestException.printStackTrace();
      }
    };

    httpClientChannel.sendAsynchronousRequest(ChannelBuffers.EMPTY_BUFFER, false, listener);

    Thread.sleep(1000);
  }

  @Test
  public void testConnectHttps() throws Exception {
    XioClientConfig xioClientConfig = XioClientConfig.newBuilder()
        .setSecurityFactory(new XioSecurityFactory() {
          @Override
          public XioSecurityHandlers getSecurityHandlers(XioServerDef def, XioServerConfig serverConfig) {
            return null;
          }

          @Override
          public XioSecurityHandlers getSecurityHandlers(XioClientConfig clientConfig) {
            return new XioSecurityHandlers() {
              @Override
              public ChannelHandler getAuthenticationHandler() {
                return null;
              }

              @Override
              public ChannelHandler getEncryptionHandler() {
                SSLEngine engine = new SSLEngineFactory("src/test/resources/privateKey.pem", "src/test/resources/cert.pem").getEngine();
                engine.setUseClientMode(true);
                SslHandler handler = new SslHandler(engine);
                return handler;
              }
            };
          }
        })
        .build();

    final Lock lock = new ReentrantLock();
    final Condition waitForFinish = lock.newCondition();

    Listener listener = new Listener() {
      @Override
      public void onRequestSent() {
        System.out.println("Request Sent");
      }

      @Override
      public void onResponseReceived(ChannelBuffer message) {
        System.out.println("Response Recieved");
        System.out.println(message.toString(Charset.defaultCharset()));
        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public void onChannelError(XioException requestException) {
        System.out.println("Request Error");
        requestException.printStackTrace();
      }
    };

    XioClient xioClient = new XioClient(xioClientConfig);
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("https://www.google.com/")));
    XioClientChannel xioClientChannel = responseFuture.get();
    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;
    httpClientChannel.sendAsynchronousRequest(ChannelBuffers.EMPTY_BUFFER, false, listener);

    lock.lock();
    waitForFinish.await();
    lock.unlock();

  }
}