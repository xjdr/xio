package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.xjeffrose.xio.client.XioConnectionPool;
import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoop;
import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoopFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * The base type of nodes over which load is balanced. Nodes define the load metric that is used;
 * distributors like P2C will use these to decide where to balance the next connection request.
 */
@Slf4j
public class Node implements Closeable {


  private final UUID token = UUID.randomUUID();
  private final ConcurrentHashMap<Channel, Stopwatch> pending = new ConcurrentHashMap<>();
  private final SocketAddress address;
  private final ImmutableList<String> filters;
  private final Stopwatch connectionStopwatch = Stopwatch.createUnstarted();
  private final List<Long> connectionTimes = new ArrayList<>();
  private final List<Long> requestTimes = new ArrayList<>();
  private final String serviceName;
  private final int weight;
  private final Protocol proto;
  private final boolean ssl;
  private final AtomicBoolean available = new AtomicBoolean(true);
  private final XioConnectionPool connectionPool;
  private final EventLoopGroup eventLoopGroup;
  private double load;

  protected EventLoopGroup eventLoopGroup() {
    return eventLoopGroup;
  }

  public Node(HostAndPort hostAndPort, Bootstrap bootstrap) {
    this(toInetAddress(hostAndPort), bootstrap);
  }

  public Node(SocketAddress address, Bootstrap bootstrap) {
    this(address, ImmutableList.of(), 0, "", Protocol.TCP, false, bootstrap);
  }

  public Node(SocketAddress address, int weight, Bootstrap bootstrap) {
    this(address, ImmutableList.of(), weight, "", Protocol.TCP, false, bootstrap);
  }

  public Node(SocketAddress address, ImmutableList<String> filters, int weight, String serviceName, Protocol proto, boolean ssl, Bootstrap bootstrap) {
    this.address = address;
    this.proto = proto;
    this.ssl = ssl;
    this.load = 0;
    this.filters = ImmutableList.copyOf(filters);
    this.weight = weight;
    this.serviceName = serviceName;
    // TODO(CK): This be passed in, we're not really taking advantage of pooling
    this.connectionPool = new XioConnectionPool(bootstrap, new AsyncRetryLoopFactory() {
      @Override
      public AsyncRetryLoop buildLoop(EventLoopGroup eventLoopGroup) {
        return new AsyncRetryLoop(3, bootstrap.config().group(), 1, TimeUnit.SECONDS);
      }
    });
    eventLoopGroup = bootstrap.config().group();
  }

  public Node(Node n) {
    this.address = n.address;
    this.load = 0;
    this.filters = ImmutableList.copyOf(n.filters);
    this.weight = n.weight;
    this.serviceName = n.serviceName;
    this.proto = Protocol.TCP;
    this.ssl = false;
    this.connectionPool = n.connectionPool;
    this.eventLoopGroup = n.eventLoopGroup;
  }

  /**
   * The current host and port returned as a InetSocketAddress
   *
   * @param hostAndPort host and port
   * @return socket address
   */
  public static InetSocketAddress toInetAddress(HostAndPort hostAndPort) {
    return (hostAndPort == null) ? null : new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
  }

  public Future<Void> send(Object message) {
    DefaultPromise<Void> promise = new DefaultPromise<>(eventLoopGroup.next());

    log.debug("Acquiring Node: " + this);
    Future<Channel> channelResult = connectionPool.acquire();
    channelResult.addListener(new FutureListener<Channel>() {
      public void operationComplete(Future<Channel> future) {
        if (future.isSuccess()) {
          Channel channel = future.getNow();
          channel.writeAndFlush(message).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) {
              if (channelFuture.isSuccess()) {
                log.debug("write finished for " + message);
                promise.setSuccess(null);
              } else {
                log.error("Write error: ", channelFuture.cause());
                promise.setFailure(channelFuture.cause());
              }
            }
          });
        } else {
          log.error("Could not connect to client for write: " + future.cause());
          promise.setFailure(future.cause());
        }
      }
    });

    return promise;
  }

  /**
   * The current load, in units of the active metric.
   *
   * @return load
   */
  public double load() {
    return load;
  }

  /**
   * The number of pending requests to this node.
   *
   * @return pending size
   */
  public int pending() {
    return pending.size();
  }

  /**
   * A token is a random integer identifying the node. It persists through node updates.
   *
   * @return token
   */
  public UUID token() {
    return token;
  }

  public InetSocketAddress address() {
    return (InetSocketAddress) address;
  }

  public void addPending(Channel channel) {
    pending.putIfAbsent(channel, Stopwatch.createStarted());
  }

  public void removePending(Channel channel) {
    if (pending.contains(channel)) {
      requestTimes.add(pending.remove(channel).elapsed(TimeUnit.MICROSECONDS));
    }
  }

  public boolean isAvailable() {
    return available.get();
  }

  public void setAvailable(boolean available) {
    this.available.set(available);
  }

  public ImmutableList<String> getFilters() {
    return filters;
  }

  public int getWeight() {
    return weight;
  }

  public String getServiceName() {
    return serviceName;
  }

  public SocketAddress getAddress() {
    return address;
  }

  public Protocol getProto() {
    return proto;
  }

  public boolean isSSL() {
    return ssl;
  }

  @Override
  public void close() throws IOException {
    // TODO(CK): Not sure what to close
  }

  @Override
  public String toString() {
    return serviceName + ": " + address();
  }
}
