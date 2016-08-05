package com.xjeffrose.xio.client.chicago;

import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoop;
import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoopFactory;
import com.xjeffrose.xio.client.XioConnectionPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.net.InetSocketAddress;

public class XioChicagoClient implements AutoCloseable {
  private final EventLoopGroup eventLoopGroup;
  private final ChicagoCluster cluster;

  private DefaultPromise<WriteResult> writeResult() {
    return new DefaultPromise<WriteResult>(eventLoopGroup.next());
  }

  private DefaultPromise<WriteResultGroup> writeResultGroup() {
    return new DefaultPromise<WriteResultGroup>(eventLoopGroup.next());
  }

  public static class XioCluster {
  }

  XioChicagoClient(Bootstrap bootstrap, ChicagoCluster cluster) {
    this.eventLoopGroup = bootstrap.config().group();
    this.cluster = cluster;
  }

  public XioChicagoClient start() {
    System.out.println("Starting");
    return this;
  }

  public void stop() {
    System.out.println("Stopping");
  }

  public Future<WriteResultGroup> write(String columnFamily, String key, String value) {
    DefaultPromise<WriteResultGroup> result = writeResultGroup();
    WriteResultGroup resultGroup = new WriteResultGroup(3);
    for(ChicagoNode node : cluster.quorumNodesForKey(key)) {
      UUID id = UUID.randomUUID();
      DefaultPromise<WriteResult> thisResult = writeResult();
      node.send(ChicagoMessage.write(id, columnFamily, key, value), thisResult);
      thisResult.addListener(new FutureListener<WriteResult>() {
        public void operationComplete(Future<WriteResult> future) {
          if (future.isSuccess()) {
            WriteResult writeResult = future.getNow();
            if (resultGroup.quorumAcheived(writeResult)) {
              result.setSuccess(resultGroup);
            }
          } else {
            // TODO retry?
            result.setFailure(future.cause());
          }
        }
      });
    }
    //result.setSuccess(new WriteResultGroup(3));
    return result;
  }

  static public XioChicagoClient newClient(XioClusterBootstrap clusterBootstrap) {
    ChicagoCluster cluster = new ChicagoCluster(clusterBootstrap);
    Bootstrap bootstrap = clusterBootstrap.config().bootstrap();
    return new XioChicagoClient(bootstrap, cluster).start();
  }

  public void close() {
    stop();
  }

  static public void main(String[] args) {
    List<InetSocketAddress> endpoints = Arrays.asList(
      new InetSocketAddress("localhost", 9001),
      new InetSocketAddress("localhost", 9002),
      new InetSocketAddress("localhost", 9003),
      new InetSocketAddress("localhost", 9004)
    );
    NioEventLoopGroup group = new NioEventLoopGroup();
    List<ChannelFuture> serverChannels = new ArrayList<ChannelFuture>();
    ServerBootstrap serverBootstrap = new ServerBootstrap()
      .group(group)
      .channel(NioServerSocketChannel.class)
      .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
          ch.pipeline()
            .addLast(new ChannelInboundHandlerAdapter() {
              @Override
              public void channelActive(ChannelHandlerContext ctx) {
                System.out.println("incoming channel active");
              }

              @Override
              public void channelRead(ChannelHandlerContext ctx, Object msg) {
                ctx.write(msg);
              }

              @Override
              public void channelReadComplete(ChannelHandlerContext ctx) {
                ctx.flush();
              }

              @Override
              public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                // Close the connection when an exception is raised.
                cause.printStackTrace();
                ctx.close();
              }
            })
          ;
        }
      })
      .childHandler(new ChannelInboundHandlerAdapter() {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
          System.out.println("Got data" + msg);
          ctx.writeAndFlush(msg);
        }
      })
    ;
    for(InetSocketAddress address : endpoints) {
      serverChannels.add(serverBootstrap.clone().localAddress(address).bind());
    }
    Bootstrap bootstrap = new Bootstrap()
      .group(group)
      .channel(NioSocketChannel.class)
    ;
    XioClusterBootstrap clusterBootstrap = new XioClusterBootstrap(bootstrap)
      .addNodes(endpoints)
      .retryLoopFactory(new AsyncRetryLoopFactory() {
        public AsyncRetryLoop buildLoop(EventLoopGroup eventLoopGroup) {
          return new AsyncRetryLoop(0, eventLoopGroup, 1, TimeUnit.MILLISECONDS);
        }
      })
    ;

    try (XioChicagoClient client = XioChicagoClient.newClient(clusterBootstrap)) {
      System.out.println("HI!!!");
      Future<WriteResultGroup> result = client.write("chicago", "key", "value");
      System.out.println("waiting");
      result.awaitUninterruptibly();
      System.out.println("done waiting");
      System.out.println("result " + result);
    }
    group.shutdownGracefully();
  }
}
