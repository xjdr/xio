package com.xjeffrose.xio.http.test_helpers;

import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.http.Http2HandlerBuilder;
import com.xjeffrose.xio.tls.SslContextFactory;
import com.xjeffrose.xio.tls.TlsConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;

public class GrpcClientTest {

  @Test
  public void testGrpcClient() throws Exception {
    EventLoopGroup group = new NioEventLoopGroup(2);
    GrpcServer server = GrpcServer.run();

    InetSocketAddress boundAddress = new InetSocketAddress("127.0.0.1", server.getPort());

    final SslContext sslContext =
        SslContextFactory.buildClientContext(
            TlsConfig.builderFrom(ConfigFactory.load().getConfig("xio.h2TestClient.settings.tls"))
                .build(),
            InsecureTrustManagerFactory.INSTANCE);

    CountDownLatch msgReceived = new CountDownLatch(2);
    Bootstrap client =
        new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(
                new ChannelInitializer<Channel>() {
                  @Override
                  protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline()
                        .addLast(
                            sslContext.newHandler(
                                ch.alloc(), boundAddress.getHostString(), boundAddress.getPort()))
                        .addLast(
                            "codec",
                            new Http2HandlerBuilder(
                                    (s) ->
                                        new Http2FrameAdapter() {
                                          @Override
                                          public int onDataRead(
                                              ChannelHandlerContext ctx,
                                              int streamId,
                                              ByteBuf data,
                                              int padding,
                                              boolean endOfStream)
                                              throws Http2Exception {
                                            msgReceived.countDown();
                                            return data.readableBytes() + padding;
                                          }

                                          @Override
                                          public void onHeadersRead(
                                              ChannelHandlerContext ctx,
                                              int streamId,
                                              Http2Headers headers,
                                              int padding,
                                              boolean endStream)
                                              throws Http2Exception {
                                            msgReceived.countDown();
                                          }

                                          @Override
                                          public void onHeadersRead(
                                              ChannelHandlerContext ctx,
                                              int streamId,
                                              Http2Headers headers,
                                              int streamDependency,
                                              short weight,
                                              boolean exclusive,
                                              int padding,
                                              boolean endStream)
                                              throws Http2Exception {
                                            msgReceived.countDown();
                                          }
                                        })
                                .server(false)
                                .build());
                  }
                })
            .remoteAddress(boundAddress);

    Channel ch = client.connect().syncUninterruptibly().channel();

    Http2Headers headers = new DefaultHttp2Headers();
    headers
        .authority("127.0.0.1:61422")
        .method("POST")
        .path("/helloworld.Greeter/SayHello")
        .scheme("https")
        .add("content-type", "application/grpc")
        .add("te", "trailers")
        .add("user-agent", "grpc-java-netty/1.7.0")
        .add("grpc-accept-encoding", "gzip")
        .add("grpc-trace-bin", "");

    ByteBuf buf = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("00000000070a05776f726c64"));
    Http2DataFrame data = new DefaultHttp2DataFrame(buf, true);
    ch.write(headers);
    ch.writeAndFlush(data).awaitUninterruptibly();
    Uninterruptibles.awaitUninterruptibly(msgReceived);
    server.stop();
  }
}
