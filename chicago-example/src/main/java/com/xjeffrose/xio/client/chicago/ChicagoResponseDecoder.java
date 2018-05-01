package com.xjeffrose.xio.client.chicago;

import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoopFactory;
import com.xjeffrose.xio.client.XioConnectionPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
//import com.google.common.util.concurrent.ListenableFuture;
//import com.google.common.util.concurrent.ListeningExecutorService;
//import com.google.common.util.concurrent.MoreExecutors;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetSocketAddress;

public class ChicagoResponseDecoder extends ByteToMessageDecoder {
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
  }
}
