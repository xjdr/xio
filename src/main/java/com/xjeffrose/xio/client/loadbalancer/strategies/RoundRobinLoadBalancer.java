package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.nashorn.internal.ir.annotations.Immutable;

public class RoundRobinLoadBalancer implements Strategy {

  private final AtomicInteger last = new AtomicInteger();

  @Override
  public boolean okToPick(Node node) {
    return true;
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
