package com.xjeffrose.xio.server;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RendezvousHash<T> {

  private final HashFunction hasher;
  private final Funnel<T> nodeFunnel;
  private final Set<T> nodeList = Sets.newConcurrentHashSet();

  public RendezvousHash(Funnel<T> nodeFunnel, Collection<? extends T> init) {
    this.hasher = Hashing.murmur3_128();
    this.nodeFunnel = nodeFunnel;

    nodeList.addAll(init);
  }

  public void add(T node) {
    nodeList.add(node);
  }

  public void remove(T node) {
    nodeList.remove(node);
  }

  public void refresh(List<T> list) {
    nodeList.clear();
    nodeList.addAll(list);
  }

  @Nullable
  public T getOne(byte[] key) {
    Map<Long, T> hashMap = Maps.newTreeMap();

    nodeList.forEach(
        node ->
            hashMap.put(
                hasher.newHasher().putBytes(key).putObject(node, nodeFunnel).hash().asLong(),
                node));

    return hashMap
        .keySet()
        .stream()
        .max(Long::compare) // find the largest key
        .map(hashMap::get) // return the largest key
        .orElse(null); // or return null if the map is empty
  }

  public List<T> get(byte[] key, int listSize) {
    Map<Long, T> hashMap = Maps.newTreeMap();
    List<T> nodes = new ArrayList<>(listSize);

    nodeList.forEach(
        node ->
            hashMap.put(
                hasher.newHasher().putBytes(key).putObject(node, nodeFunnel).hash().asLong(),
                node));

    TreeSet<Long> set = Sets.newTreeSet(hashMap.keySet());

    for (int i = 0; i < listSize; i++) {
      Long x = set.first();
      nodes.add(i, hashMap.remove(x));
      set.remove(x);
    }

    return nodes;
  }
}
