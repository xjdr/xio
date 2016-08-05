package com.xjeffrose.xio.client.chicago;

import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoopFactory;
import com.xjeffrose.xio.client.XioConnectionPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetSocketAddress;

public class ChicagoCluster {
  private final ConcurrentMap<InetSocketAddress, ChicagoNode> nodeMap;
  private static ChicagoNode buildNode(XioClusterBootstrap clusterBootstrap, InetSocketAddress address) {
    return new ChicagoNode(clusterBootstrap, address);
  }

  public ChicagoCluster(XioClusterBootstrap clusterBootstrap) {
    nodeMap = new ConcurrentHashMap<>();
    for (InetSocketAddress address : clusterBootstrap.config().nodes()) {
      nodeMap.put(address, buildNode(clusterBootstrap, address));
    }
  }

  public Collection<ChicagoNode> quorumNodesForKey(String key) {
    return nodeMap.values();
  }
}
