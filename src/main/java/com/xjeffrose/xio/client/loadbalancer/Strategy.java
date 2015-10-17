package com.xjeffrose.xio.client.loadbalancer;

import java.util.Vector;

public interface Strategy {

  Node getNextNode(Vector<Node> pool);
}
