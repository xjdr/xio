package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;

/**
 * The base type of nodes over which load is balanced. Nodes define the load metric that is used;
 * distributors like P2C will use these to decide where to balance the next connection request.
 */
public class Node {
  private static final Logger log = Logger.getLogger(Node.class);

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
  private double load;
  private final String hostname;

  public Node(HostAndPort hostAndPort) {
    this(toInetAddress(hostAndPort));
  }

  public Node(SocketAddress address) {
    this(address.toString(), address, ImmutableList.of(), 0, "", Protocol.TCP, false);
  }

  public Node(SocketAddress address, int weight) {
    this(address.toString(), address, ImmutableList.of(), weight, "", Protocol.TCP, false);
  }

  public Node(SocketAddress address, ImmutableList<String> filters, int weight, java.lang.String serviceName, Protocol proto, boolean ssl) {
    this(address.toString(),address,filters,weight,serviceName,proto,ssl);
  }

  public Node(String hostname, SocketAddress address, ImmutableList<String> filters, int weight, java.lang.String serviceName, Protocol proto, boolean ssl) {
    this.address = address;
    this.proto = proto;
    this.ssl = ssl;
    this.load = 0;
    this.filters = ImmutableList.copyOf(filters);
    this.weight = weight;
    this.serviceName = serviceName;
    this.hostname = hostname;
  }

  public Node(Node n) {
    this.address = n.address;
    this.load = 0;
    this.filters = ImmutableList.copyOf(n.filters);
    this.weight = n.weight;
    this.serviceName = n.serviceName;
    this.proto = Protocol.TCP;
    this.ssl = false;
    this.hostname = n.hostname;
  }

  /**
   * . The current host and port returned as a InetSocketAddress
   */
  public static InetSocketAddress toInetAddress(HostAndPort hostAndPort) {
    return (hostAndPort == null) ? null : new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
  }

  /**
   * The current load, in units of the active metric.
   */
  public double load() {
    return load;
  }

  /**
   * The number of pending requests to this node.
   */
  public int pending() {
    return pending.size();
  }

  /**
   * A token is a random integer identifying the node. It persists through node updates.
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

  public String getHostname() { return hostname; }

  public java.lang.String getServiceName() {
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
  public java.lang.String  toString() {
    return serviceName + ": " + address() + " : "+ proto.toString() + ": SSL="+isSSL() + ":  Healthy="+isAvailable();
  }
}
