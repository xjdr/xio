package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    List<UUID> ids = new ArrayList<>(okNodes.keySet());

    Collections.shuffle(ids, new Random());
    for (UUID id: ids) {
      Node nextNode = okNodes.get(id);
      if (okToPick(nextNode)) {
        if (nextNode.isAvailable()) {
          return nextNode;
        }
      }
    }
    return null;
  }
}
