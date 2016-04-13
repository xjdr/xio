package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.collect.ImmutableList;

public interface Strategy {

  Node getNextNode(ImmutableList<Node> pool);
  public boolean okToPick(Node node);
}
