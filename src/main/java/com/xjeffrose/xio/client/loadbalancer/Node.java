package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.net.HostAndPort;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.Vector;

/**
 * The base type of nodes over which load is balanced. Nodes define the load metric that is used;
 * distributors like P2C will use these to decide where to balance the next connection request.
 */
public class Node {

  private final UUID token = UUID.randomUUID();
  private final Vector<Channel> pending = new Vector<>();
  private final SocketAddress address;
  private double load;

  public Node(HostAndPort hostAndPort) {
    this(toInetAddress(hostAndPort));
  }

  public Node(SocketAddress address) {
    this.address = address;
    this.load = 0;
  }

  /**
   * The current host and port returned as a InetSocketAddress
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

  public SocketAddress address() {
    return address;
  }

  public void pending(Channel channel) {
    pending.add(channel);
  }

  public boolean isAvailable() {
    //TODO(JR): Find better way to determine this
    return true;
  }
}
