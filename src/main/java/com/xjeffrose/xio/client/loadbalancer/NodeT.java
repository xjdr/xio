package com.xjeffrose.xio.client.loadbalancer;

public interface NodeT {

  /**
   * The base type of nodes over which load is balanced.
   * Nodes define the load metric that is used; distributors
   * like P2C will use these to decide where to balance
   * the next connection request.
   */


  /**
   * The current load, in units of the active metric.
   */
  double load();

  /**
   * The number of pending requests to this node.
   */
  int pending();

  /**
   * A token is a random integer identifying the node. It persists through node updates.
   */
  int token();

  /**
   * The underlying service factory.
   */
//      ServiceFactory factory();


}
