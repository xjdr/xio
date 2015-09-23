package com.xjeffrose.xio.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.core.XioException;
import com.xjeffrose.xio.core.XioNoOpHandler;
import com.xjeffrose.xio.core.XioSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import com.xjeffrose.xio.core.XioTimer;
import com.xjeffrose.xio.core.XioTransportException;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class XioClientTest {
  public static final XioTimer timer = new XioTimer("Test Timer", (long) 100, TimeUnit.MILLISECONDS, 100);

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
                SslContext sslCtx = SslContext.newClientContext(SslContext.defaultClientProvider(), InsecureTrustManagerFactory.INSTANCE);
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
  public void testCall2() throws Exception {

  }

  @Test
  public void testCall3() throws Exception {

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

    // Lets make a HTTP parser cause apparently that's a good idea...
    ByteBuf response = listener.getResponse();
    String[] headerBody = response.toString(Charset.defaultCharset()).split("\r\n\r\n");
    String[] headers = headerBody[0].split("\r\n");
    String[] firstLine = headers[0].split("\\s");

    // Lets make a HTTP Response object now
    DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
        HttpVersion.valueOf(firstLine[0]),
        new HttpResponseStatus(Integer.parseInt(firstLine[1]), firstLine[2]),
        httpClientChannel.getCtx().alloc().buffer().writeBytes(headerBody[1].getBytes()));

    for (int i = 1; i < headers.length; i++) {
      String[] xs = headers[i].split(":");
      httpResponse.headers().add(xs[0].trim(), xs[1].trim());
    }

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

    Listener listener = new Listener() {
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


    // Lets make a HTTP parser cause apparently that's a good idea...
    ByteBuf response = listener.getResponse();
    String[] headerBody = response.toString(Charset.defaultCharset()).split("\r\n\r\n");
    String[] headers = headerBody[0].split("\r\n");
    String[] firstLine = headers[0].split("\\s");

    // Lets make a HTTP Response object now
    DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
        HttpVersion.valueOf(firstLine[0]),
        new HttpResponseStatus(Integer.parseInt(firstLine[1]), firstLine[2]),
        httpClientChannel.getCtx().alloc().buffer().writeBytes(headerBody[1].getBytes()));

    for (int i = 1; i < headers.length; i++) {
      String[] xs = headers[i].split(":");
      httpResponse.headers().add(xs[0].trim(), xs[1].trim());
    }

    //Now we have something that we can actually test ...
    assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
    assertEquals("nginx/1.6.0", httpResponse.headers().get("Server"));
    assertTrue(httpResponse.content() != null);

  }
}