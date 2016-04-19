package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.xjeffrose.xio.client.loadbalancer.Filter;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
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
  public Node getNextNode(ImmutableList<Node> pool, Map<UUID, Node> okNodes) {
    if (okNodes.isEmpty()) {
      return null;
    }

    ImmutableList<UUID> _ok = ImmutableList.copyOf(okNodes.keySet());
    Node nextNode = okNodes.get( _ok.get(new Random().nextInt(okNodes.size())));

    if (okToPick(nextNode)) {
      return nextNode;
    }

    return getNextNode(pool, okNodes);
  }
}
