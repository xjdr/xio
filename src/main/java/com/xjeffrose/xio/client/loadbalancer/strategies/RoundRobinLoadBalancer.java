package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements Strategy {

  private final AtomicInteger last = new AtomicInteger();

  @Override
  public boolean okToPick(Node node) {
    return true;
  }

  @Override
  public Node getNextNode(ImmutableList<Node> pool) {
    if (pool.isEmpty()) {
      return null;
    }

    if (last.get() == pool.size()) {
      last.set(0);
      return pool.get(0);
    } else {
      int next = last.getAndIncrement();
      if (next <= pool.size()) {
        return pool.get(next);
      } else {
        return null;
      }
    }
  }

}
