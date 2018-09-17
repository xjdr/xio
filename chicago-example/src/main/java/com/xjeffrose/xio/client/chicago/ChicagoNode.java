package com.xjeffrose.xio.client.chicago;

import com.xjeffrose.xio.client.XioConnectionPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChicagoNode {
  private final XioConnectionPool connectionPool;
  private final ConcurrentMap<UUID, Promise<WriteResult>> resultMap;

  private CombinedChannelDuplexHandler<ChicagoResponseDecoder, ChicagoRequestEncoder>
      newMessageHandler() {
    return new CombinedChannelDuplexHandler<ChicagoResponseDecoder, ChicagoRequestEncoder>(
        new ChicagoResponseDecoder(), new ChicagoRequestEncoder());
  }

  private CombinedChannelDuplexHandler<Murmur3HashedFrameDecoder, Murmur3HashedFrameEncoder>
      newMurmur3HashHandler() {
    return new CombinedChannelDuplexHandler<Murmur3HashedFrameDecoder, Murmur3HashedFrameEncoder>(
        new Murmur3HashedFrameDecoder(), new Murmur3HashedFrameEncoder());
  }

  private CombinedChannelDuplexHandler<LengthFieldBasedFrameDecoder, LengthFieldPrepender>
      newLengthFieldBasedFrameHandler() {
    return new CombinedChannelDuplexHandler<LengthFieldBasedFrameDecoder, LengthFieldPrepender>(
        new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2), new LengthFieldPrepender(2));
  }

  private SimpleChannelInboundHandler<ChicagoMessage> newReader() {
    return new SimpleChannelInboundHandler<ChicagoMessage>() {
      @Override
      protected void channelRead0(ChannelHandlerContext ctx, ChicagoMessage msg) throws Exception {
        Promise<WriteResult> result = resultMap.get(msg.id);
        if (result != null) {
          System.out.println("Got result for id " + msg.id);
          result.setSuccess(new WriteResult());
        } else {
          System.out.println("Couldn't find result for id " + msg.id);
        }
      }
    };
  }

  private ChannelInitializer<SocketChannel> newHandler() {
    return new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel channel) throws Exception {
        channel
            .pipeline()
            // input check the hash, output create the hash
            .addLast(newMurmur3HashHandler())
            // input handle the message, output create the message
            .addLast(newMessageHandler())
            .addLast(newReader());
      }
    };
  }

  private XioConnectionPool buildPool(
      XioClusterBootstrap.Config config, InetSocketAddress address) {
    Bootstrap bootstrap = config.bootstrap().clone().remoteAddress(address).handler(newHandler());

    return new XioConnectionPool(bootstrap, config.retryLoopFactory());
  }

  public ChicagoNode(XioClusterBootstrap clusterBootstrap, InetSocketAddress address) {
    resultMap = new ConcurrentHashMap<>();
    connectionPool = buildPool(clusterBootstrap.config(), address);
  }

  public Future<WriteResult> send(ChicagoMessage message, Promise<WriteResult> result) {
    // TODO schedule a timeout to fail this write
    resultMap.put(message.id, result);
    Future<Channel> channelResult = connectionPool.acquire();
    System.out.println("Acquiring Node");
    channelResult.addListener(
        new FutureListener<Channel>() {
          public void operationComplete(Future<Channel> future) {
            if (future.isSuccess()) {
              System.out.println("Node acquired!");
              Channel channel = future.getNow();
              // TODO could maybe put a listener here to track successful writes
              channel
                  .writeAndFlush(message)
                  .addListener(
                      new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture channelFuture) {
                          System.out.println("write finished for " + message.id);
                        }
                      });
            } else {
              result.setFailure(future.cause());
            }
          }
        });
    return result;
  }
}
