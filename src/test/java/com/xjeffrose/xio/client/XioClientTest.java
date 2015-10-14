package com.xjeffrose.xio.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.core.BBtoHttpResponse;
import com.xjeffrose.xio.core.XioException;
import com.xjeffrose.xio.core.XioNoOpHandler;
import com.xjeffrose.xio.core.XioSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import com.xjeffrose.xio.core.XioTimer;
import com.xjeffrose.xio.fixtures.TcpServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLException;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class XioClientTest {
  public static final XioTimer timer = new XioTimer("Test Timer", (long) 100, TimeUnit.MILLISECONDS, 100);
  private static final Logger log = Logger.getLogger(XioClientTest.class.getName());
  final XioClientConfig xioClientConfig = XioClientConfig.newBuilder()
      .setSecurityFactory(new XioSecurityFactory() {
        @Override
        public XioSecurityHandlers getSecurityHandlers(XioServerDef def, XioServerConfig serverConfig) {
          return null;
        }

        @Override
        public XioSecurityHandlers getSecurityHandlers() {
          return new XioSecurityHandlers() {
            @Override
            public ChannelHandler getAuthenticationHandler() {
              return new XioNoOpHandler();
            }

            @Override
            public ChannelHandler getEncryptionHandler() {
              try {
                SslContext sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

                return sslCtx.newHandler(new PooledByteBufAllocator());
              } catch (SSLException e) {
                e.printStackTrace();
              }

              return null;
            }
          };
        }
      })
      .build();


  @Test
  public void testCall() throws Exception {
    DefaultFullHttpResponse httpResponse = XioClient.call(new URI("http://www.google.com/"));

    assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
    assertEquals("gws", httpResponse.headers().get("Server"));
    assertTrue(httpResponse.content() != null);
  }

  //  @Test(expected = XioTransportException.class)
  @Test
  public void testBadCall() throws Exception {

    DefaultFullHttpResponse httpResponse = XioClient.call(new URI("https://www.google.com/"));

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, httpResponse.getStatus());
//    assertEquals("gws", httpResponse.headers().get("Server"));
    assertTrue(httpResponse.content() != null);

  }

  @Test
  public void testCall1() throws Exception {

    DefaultFullHttpResponse httpResponse = XioClient.call(xioClientConfig, new URI("https://www.paypal.com/home"));
    assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
    assertEquals("nginx/1.6.0", httpResponse.headers().get("Server"));
    assertTrue(httpResponse.content() != null);
  }

  @Test
  public void testAsyncHttp() throws Exception {

    final Lock lock = new ReentrantLock();
    final Condition waitForFinish = lock.newCondition();

    XioClient xioClient = new XioClient();
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("http://www.google.com/")));
    XioClientChannel xioClientChannel = responseFuture.get();
    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

    Listener listener = new Listener<ByteBuf>() {
      ByteBuf response;

      @Override
      public void onRequestSent() {
        //For debug only
//        System.out.println("Request Sent");
      }

      @Override
      public void onResponseReceived(ByteBuf message) {
        response = message;
        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public void onChannelError(XioException requestException) {
        StringBuilder sb = new StringBuilder();
        sb.append(HttpVersion.HTTP_1_1)
            .append(" ")
            .append(HttpResponseStatus.INTERNAL_SERVER_ERROR)
            .append("\r\n")
            .append("\r\n\r\n")
            .append(requestException.getMessage())
            .append("\n");

        response = Unpooled.wrappedBuffer(sb.toString().getBytes());

        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public ByteBuf getResponse() {
        return response;
      }

    };

    httpClientChannel.sendAsynchronousRequest(Unpooled.EMPTY_BUFFER, false, listener);

    lock.lock();
    waitForFinish.await();
    lock.unlock();

    DefaultFullHttpResponse httpResponse = BBtoHttpResponse.getResponse(listener.getResponse());

    //Now we have something that we can actually test ...
    assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
    assertEquals("gws", httpResponse.headers().get("Server"));
    assertTrue(httpResponse.content() != null);

  }

  @Test
  public void testAsyncHttps() throws Exception {
    final Lock lock = new ReentrantLock();
    final Condition waitForFinish = lock.newCondition();
    final XioClient xioClient = new XioClient(xioClientConfig);

    Listener listener = new Listener<ByteBufHolder>() {
      ByteBufHolder response;

      @Override
      public void onRequestSent() {
        //For debug only
//        System.out.println("Request Sent");
      }

      @Override
      public void onResponseReceived(ByteBufHolder message) {
        response = message;

        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public void onChannelError(XioException requestException) {
        StringBuilder sb = new StringBuilder();
        sb.append(HttpVersion.HTTP_1_1)
            .append(" ")
            .append(HttpResponseStatus.INTERNAL_SERVER_ERROR)
            .append("\r\n")
            .append("\r\n\r\n")
            .append(requestException.getMessage())
            .append("\n");

        response = (ByteBufHolder) Unpooled.wrappedBuffer(sb.toString().getBytes());

        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public ByteBufHolder getResponse() {
        return response;
      }
    };

    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("https://www.paypal.com/home")));
    XioClientChannel xioClientChannel = responseFuture.get();
    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;
    Map<String, String> headerMap = ImmutableMap.of(
        HttpHeaders.HOST, "www.paypal.com",
        HttpHeaders.USER_AGENT, "xio/0.7.8",
        HttpHeaders.CONTENT_TYPE, "application/text",
        HttpHeaders.ACCEPT_ENCODING, "*/*"
    );

    httpClientChannel.setHeaders(headerMap);
    httpClientChannel.sendAsynchronousRequest(Unpooled.EMPTY_BUFFER, false, listener);

    lock.lock();
    waitForFinish.await();
    lock.unlock();

    DefaultFullHttpResponse httpResponse = BBtoHttpResponse.getResponse(listener.getResponse());

    //Now we have something that we can actually test ...
    assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
    assertEquals("nginx/1.6.0", httpResponse.headers().get("Server"));
    assertTrue(httpResponse.content() != null);

  }

  @Test
  public void testTcp() throws Exception {
    TcpServer tcpServer = new TcpServer(8100);
    new Thread(tcpServer).start();

    Thread.sleep(100);

    final Lock lock = new ReentrantLock();
    final Condition waitForFinish = lock.newCondition();

    XioClient xioClient = new XioClient();
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new TcpClientConnector("127.0.0.1", 8100));
    XioClientChannel xioClientChannel = responseFuture.get();
    TcpClientChannel tcpClientChannel = (TcpClientChannel) xioClientChannel;

    Listener listener = new Listener() {
      ByteBuf response;

      @Override
      public void onRequestSent() {
        //For debug only
//        log.error("Request Sent");
      }

      @Override
      public void onResponseReceived(Object message) {
        response = message;
        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public void onChannelError(XioException requestException) {
        log.error("Error", requestException);

        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public ByteBuf getResponse() {
        return response;
      }

    };

    tcpClientChannel.sendAsynchronousRequest(Unpooled.wrappedBuffer("Working Tcp Proxy\n".getBytes()), false, listener);

    lock.lock();
    waitForFinish.await();
    lock.unlock();

    assertEquals("Working Tcp Proxy\n", listener.getResponse().toString(Charset.defaultCharset()));
  }
}