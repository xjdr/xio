package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.UUID;

public interface Strategy {

  boolean okToPick(Node node);

  Node getNextNode(ImmutableList<Node> pool, Map<UUID, Node> okNodes);
}
