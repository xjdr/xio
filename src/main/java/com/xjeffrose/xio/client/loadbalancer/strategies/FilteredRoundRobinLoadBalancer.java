package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.xjeffrose.xio.client.loadbalancer.Filter;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import java.util.concurrent.atomic.AtomicInteger;

public class FilteredRoundRobinLoadBalancer implements Strategy {
  private final AtomicInteger last = new AtomicInteger();
  private final Filter filter;

  public FilteredRoundRobinLoadBalancer(Filter filter) {
    this.filter = filter;
  }

  @Override
  public boolean okToPick(Node node) {
    return node.getFilters().stream().allMatch(item -> filter.contains(node.getServiceName(), node.address().getHostName(), item));
  }

  @Override
  public Node getNextNode(ImmutableList<Node> pool) {
    if (pool.isEmpty()) {
      return null;
    }

    return getNextNode(pool, 0);
  }

  private Node getNextNode(ImmutableList<Node> pool, int overflow) {

    if (overflow == pool.size()) {
      return null;
    }

    overflow++;

    int idx = last.getAndIncrement();
    if (idx == pool.size()) {
      last.set(0);
      idx = 0;
    }
    Node nextNode = pool.get(idx);
    if (okToPick(nextNode)) {
      return nextNode;
    } else {
      return getNextNode(pool, overflow);
    }
  }

}
