package com.xjeffrose.xio.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.strategies.RoundRobinLoadBalancer;
import com.xjeffrose.xio.client.retry.BoundedExponentialBackoffRetry;
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
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Vector;
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
  public void testAsyncHttp() throws Exception {

    final Lock lock = new ReentrantLock();
    final Condition waitForFinish = lock.newCondition();

    XioClient xioClient = new XioClient();
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("http://www.google.com/")),  new BoundedExponentialBackoffRetry(1000, 100000, 3));
    XioClientChannel xioClientChannel = responseFuture.get();
    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

    Listener<ByteBuf> listener = new Listener<ByteBuf>() {
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

    Listener<ByteBuf> listener = new Listener<ByteBuf>() {
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

        response = (ByteBuf) Unpooled.wrappedBuffer(sb.toString().getBytes());

        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public ByteBuf getResponse() {
        return response;
      }
    };

    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("https://www.paypal.com/home")),  new BoundedExponentialBackoffRetry(1000, 100000, 3));
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
//    assertEquals("nginx/1.6.0", httpResponse.headers().get("Server"));
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
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new TcpClientConnector("127.0.0.1", 8100),  new BoundedExponentialBackoffRetry(1000, 100000, 3));
    XioClientChannel xioClientChannel = responseFuture.get();
    TcpClientChannel tcpClientChannel = (TcpClientChannel) xioClientChannel;

    Listener<ByteBuf> listener = new Listener<ByteBuf>() {
      ByteBuf response;

      @Override
      public void onRequestSent() {
        //For debug only
//        log.error("Request Sent");
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


  @Test
  public void testTCPLB() throws Exception {
    TcpServer tcpServer1 = new TcpServer(9110);
    TcpServer tcpServer2 = new TcpServer(9120);
    TcpServer tcpServer3 = new TcpServer(9130);

    new Thread(tcpServer1).start();
    new Thread(tcpServer2).start();
    new Thread(tcpServer3).start();

    final Lock lock = new ReentrantLock();
    final Condition waitForFinish = lock.newCondition();

    final RoundRobinLoadBalancer strategy = new RoundRobinLoadBalancer();
    final ImmutableList<Node> pool = ImmutableList.of(new Node(new InetSocketAddress("127.0.0.1", 9110)), new Node(new InetSocketAddress("127.0.0.1", 9120)), new Node(new InetSocketAddress("127.0.0.1", 9130)));
    final Distributor distributor = new Distributor(pool, strategy);

    XioClient xioClient = new XioClient();
    ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(new TcpClientConnector(distributor.pick().address()),  new BoundedExponentialBackoffRetry(1000, 100000, 3));
    XioClientChannel xioClientChannel = responseFuture.get();
    TcpClientChannel tcpClientChannel = (TcpClientChannel) xioClientChannel;

    Listener<ByteBuf> listener = new Listener<ByteBuf>() {
      ByteBuf response;

      @Override
      public void onRequestSent() {
        //For debug only
//        log.error("Request Sent");
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
        log.error("Error", requestException);

        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();

        response.release();
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
    assertEquals( 9120 , distributor.pick().address().getPort());
    assertEquals( 9130 , distributor.pick().address().getPort());
    assertEquals( 9110 , distributor.pick().address().getPort());
  }
}