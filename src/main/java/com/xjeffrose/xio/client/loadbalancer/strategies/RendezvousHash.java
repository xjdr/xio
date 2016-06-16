package com.xjeffrose.xio.client.loadbalancer.strategies;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.log4j.Logger;


public class RendezvousHash<N> {
  private static final Logger log = Logger.getLogger(RendezvousHash.class.getName());

  private final HashFunction hasher;
  private final Funnel<N> nodeFunnel;
  private final int quorum;

  private ConcurrentSkipListSet<N> nodeList;

  public RendezvousHash(Funnel<N> nodeFunnel, Collection<N> init, int quorum) {
    this.hasher = Hashing.murmur3_128();
    this.nodeFunnel = nodeFunnel;
    this.nodeList = new ConcurrentSkipListSet<>(init);
    this.quorum = quorum;
  }

  boolean remove(N node) {
    return nodeList.remove(node);
  }

  boolean add(N node) {
    return nodeList.add(node);
  }

  public List<N> get(byte[] key) {
    while (nodeList.size() < quorum) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Map<Long, N> hashMap = new ConcurrentHashMap<>();
    List<N> _nodeList = new ArrayList<>();

    nodeList.stream()
      .filter(xs -> !_nodeList.contains(xs))
      .forEach(xs -> {
        hashMap.put(hasher.newHasher()
          .putBytes(key)
          .putObject(xs, nodeFunnel)
          .hash().asLong(), xs);

      });

    for (int i = 0; i < quorum; i++) {
      _nodeList.add(hashMap.remove(hashMap.keySet().stream().max(Long::compare).orElse(null)));
    }

    return _nodeList;
  }

  public void refresh(List<N> list) {
    nodeList = new ConcurrentSkipListSet<>(list);
  }
}
