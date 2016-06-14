package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnels;
import com.xjeffrose.xio.client.RendezvousHash;
import com.xjeffrose.xio.client.loadbalancer.Filter;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Strategy;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsistantHashLoadBalancer implements Strategy {

  public ConsistantHashLoadBalancer() {
  }

  @Override
  public boolean okToPick(Node node) {
    return true;
  }

  @Override
  public Node getNextNode(ImmutableList<Node> pool, Map<UUID, Node> okNodes) {

//    List<String> nodeList = new ArrayList<>();
//
//    pool.stream().filter(xs -> okNodes.containsValue(xs)).forEach(xs -> nodeList.add(((InetSocketAddress) xs.getAddress()).getHostString()));
//
//    RendezvousHash rendezvousHash = new RendezvousHash(Funnels.stringFunnel(Charset.defaultCharset()), nodeList, 1);
//    return rendezvousHash.get();

    // Need to pass in req val to hash against for consistent hashing
    return null;
  }
}
