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

    if (last.get() == pool.size() && okToPick(pool.get(0))) {
      last.set(0);
      return pool.get(0);
    } else {
      Node nextNode = pool.get(last.getAndIncrement());
      if (okToPick(nextNode)) {
        return nextNode;
      } else {
        getNextNode(pool);
      }
    }
    return null;
  }

}
