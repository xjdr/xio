package com.xjeffrose.xio.client.chicago;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChicagoCluster {
  private final ConcurrentMap<InetSocketAddress, ChicagoNode> nodeMap;

  private static ChicagoNode buildNode(
      XioClusterBootstrap clusterBootstrap, InetSocketAddress address) {
    return new ChicagoNode(clusterBootstrap, address);
  }

  public ChicagoCluster(XioClusterBootstrap clusterBootstrap) {
    nodeMap = new ConcurrentHashMap<>();
    for (InetSocketAddress address : clusterBootstrap.config().nodes()) {
      nodeMap.put(address, buildNode(clusterBootstrap, address));
    }
  }

  public Collection<ChicagoNode> quorumNodesForKey(String key) {
    return nodeMap.values();
  }
}
