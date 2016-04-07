package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements Strategy {

  AtomicInteger last = new AtomicInteger();

  @Override
  public Node getNextNode(ImmutableList<Node> pool) {
    if (last.get() == pool.size()) {
      last.set(0);
      return pool.get(0);
    } else {
      return pool.get(last.getAndIncrement());
    }
  }

}
