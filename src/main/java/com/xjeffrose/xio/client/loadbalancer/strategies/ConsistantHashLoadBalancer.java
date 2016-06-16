package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnels;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConsistantHashLoadBalancer implements Strategy {

  public ConsistantHashLoadBalancer() {
  }

  @Override
  public boolean okToPick(Node node) {
    return true;
  }

  @Override
  public Node getNextNode(ImmutableList<Node> pool, Map<UUID, Node> okNodes) {
    return null;
  }

  @Override
  public Node getNextNode(ImmutableList<Node> pool, Map<UUID, Node> okNodes, String sessionID) {
    List<String> idStrings = new ArrayList<>();
    okNodes.keySet().stream().forEach(xs -> idStrings.add(xs.toString()));

    RendezvousHash rendezvousHash = new RendezvousHash(Funnels.stringFunnel(Charset.defaultCharset()), idStrings, idStrings.size());

    for (Object nodeID : rendezvousHash.get(sessionID.getBytes())) {
      if (nodeID instanceof String) {
        UUID id = UUID.fromString((String) nodeID);
        Node nextNode = okNodes.get(id);
        if (okToPick(nextNode)) {
          return nextNode;
        }
      } else {
        return null;
      }
    }

    return null;
  }
}
