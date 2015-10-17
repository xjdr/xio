package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.net.HostAndPort;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The base type of nodes over which load is balanced. Nodes define the load metric that is used;
 * distributors like P2C will use these to decide where to balance the next connection request.
 */
public class Node {

  private int token;
  private double load;
  private AtomicInteger pending;
  private HostAndPort hostAndPort;

  public Node(HostAndPort hostAndPort) {
    this.hostAndPort = hostAndPort;
  }

//  public Node newNode() {
//    return new Node();
//  }
//
//  protected Node failingNode() {
//    return new Node();
//  }

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
    return pending.get();
  }

  /**
   * A token is a random integer identifying the node. It persists through node updates.
   */
  public int token() {
    return token;
  }

}
