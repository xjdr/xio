package com.xjeffrose.xio.clientBak;

import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.core.XioSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import com.xjeffrose.xio.fixtures.SimpleTestServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLException;
import org.junit.Test;

public class XioClientTest {

  @Test
  public void testHttp() throws Exception {

    SimpleTestServer testServer = new SimpleTestServer(8082);
    testServer.run();

    XioClient xioClient = new XioClient();
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("http://localhost:8082")));
    XioClientChannel xioClientChannel = responseFuture.get();
    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

    ByteBuf resp = httpClientChannel.sendProxyRequest(Unpooled.EMPTY_BUFFER);

    System.out.println(resp.toString(Charset.defaultCharset()));

    testServer.stop();

  }

  @Test
  public void testAsyncHttp() throws Exception {

    final Lock lock = new ReentrantLock();
    final Condition waitForFinish = lock.newCondition();

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
      public void onResponseReceived(ByteBuf message) {
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

    httpClientChannel.sendAsynchronousRequest(Unpooled.EMPTY_BUFFER, false, listener);

    lock.lock();
    waitForFinish.await();
    lock.unlock();
  }

  @Test
  public void testAsyncHttps() throws Exception {
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
//                SSLEngine engine = null;
                try {
                  SslContext sslCtx = SslContext.newClientContext(SslContext.defaultClientProvider(), InsecureTrustManagerFactory.INSTANCE);
//                  SSLEngine engine = new SSLEngineFactory(true).getEngine();
//                  engine.beginHandshake();
                  return sslCtx.newHandler(new PooledByteBufAllocator());
                } catch (SSLException e) {
                  e.printStackTrace();
                }
                return null;
//                return new SslHandler(engine, false);
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
      public void onResponseReceived(ByteBuf message) {
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
    httpClientChannel.sendAsynchronousRequest(Unpooled.EMPTY_BUFFER, false, listener);

    lock.lock();
    waitForFinish.await();
    lock.unlock();

  }
}