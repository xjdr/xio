package com.xjeffrose.xio.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

@Slf4j
public class ZkClient implements ConfigurationProvider {

  private LeaderSelector leaderSelector;
  private final XioLeaderSelectorListener leaderListener = new XioLeaderSelectorListener();
  private CuratorFramework client;
  private String connectionString;
  private Map<String, NodeCache> nodeCaches = new HashMap<>();
  private Map<String, TreeCache> treeCaches = new HashMap<>();

  public ZkClient(CuratorFramework client) {
    this.client = client;
    this.connectionString = client.getZookeeperClient().getCurrentConnectionString();
  }

  public ZkClient(String serverSet) {
    connectionString = serverSet;
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    client = CuratorFrameworkFactory.newClient(serverSet, retryPolicy);
  }

  // Used by NullZkClient
  protected ZkClient() {}

  public void rebuild() {
    client.close();
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 4);
    client = CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
  }

  public void register(String NODE_LIST_PATH, InetSocketAddress address, @Nullable byte[] data) {
    try {
      client
          .create()
          .creatingParentsIfNeeded()
          .withMode(CreateMode.EPHEMERAL)
          .forPath(
              NODE_LIST_PATH
                  + "/"
                  + address.getAddress().getHostAddress()
                  + ":"
                  + address.getPort(),
              data);
    } catch (Exception e) {
      log.error("Error registering Server", e);
      throw new RuntimeException(e);
    }
  }

  public void register(String NODE_LIST_PATH, String ip, int port, @Nullable byte[] data) {
    register(NODE_LIST_PATH, new InetSocketAddress(ip, port), data);
  }

  public void register(String path, InetSocketAddress address) {
    register(path, address, null);
  }

  public void electLeader(String ELECTION_PATH, LeaderSelectorListener listener) {
    leaderSelector = new LeaderSelector(client, ELECTION_PATH, listener);

    leaderSelector.autoRequeue();
    leaderSelector.start();
  }

  public void electLeader(String path) {}

  public void startNodeCache(NodeCache cache) {
    try {
      cache.start();
    } catch (Exception e) {
      log.error("Error starting nodeCache {}", cache, e);
      throw new RuntimeException(e);
    }
  }

  public void startTreeCache(TreeCache cache) {
    try {
      cache.start();
    } catch (Exception e) {
      log.error("Error starting treeCache {}", cache, e);
      throw new RuntimeException(e);
    }
  }

  public void start() {
    try {
      client.start();
      client.blockUntilConnected();
      nodeCaches.values().forEach(this::startNodeCache);
      treeCaches.values().forEach(this::startTreeCache);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopNodeCache(NodeCache cache) {
    try {
      cache.close();
    } catch (IOException e) {
      log.error("Error stopping nodeCache {}", cache, e);
      throw new RuntimeException(e);
    }
  }

  public void stop() throws Exception {
    leaderListener.relinquish();
    if (leaderSelector != null) {
      leaderSelector.close();
    }
    nodeCaches.values().forEach(this::stopNodeCache);
    treeCaches.values().forEach(TreeCache::close);
    client.close();
  }

  public void set(String path, String data) {
    try {
      client.create().creatingParentsIfNeeded().forPath(path, data.getBytes());
    } catch (Exception e) {
      // throw new RuntimeException(e);
    }
  }

  public String get(String path) {
    try {
      return new String(client.getData().forPath(path), Charset.forName("UTF-8"));
    } catch (Exception e) {
      // TODO: I need to deal with the error better
      //      log.severe("No node for for: " + path);
      //      throw new RuntimeException(e);
    }
    return null;
  }

  public void set(String path, String data, boolean compress) {
    try {
      client.create().compressed().creatingParentsIfNeeded().forPath(path, data.getBytes());
    } catch (Exception e) {
      // throw new RuntimeException(e);
    }
  }

  public String get(String path, boolean compress) {
    try {
      return new String(client.getData().decompressed().forPath(path), Charset.forName("UTF-8"));
    } catch (Exception e) {
      // throw new RuntimeException(e);
    }
    return null;
  }

  public List<String> list(String path) {
    try {
      return client.getChildren().forPath(path);
    } catch (Exception e) {
      // throw new RuntimeException(e);
    }
    return null;
  }

  public List<String> getChildren(String path) {
    try {
      return client.getChildren().forPath(path);
    } catch (Exception e) {
      // TODO: I need to deal with the error better
      //      log.severe("No node for for: " + path);
      //      throw new RuntimeException(e);
    }
    return null;
  }

  /* package access only */
  public CuratorFramework getClient() {
    return client;
  }

  public String getConnectionString() {
    return connectionString;
  }

  private NodeCache getOrCreateNodeCache(String path) {
    NodeCache cache;

    if (nodeCaches.containsKey(path)) {
      cache = nodeCaches.get(path);
    } else {
      cache = new NodeCache(client, path);
      nodeCaches.put(path, cache);
    }
    return cache;
  }

  private TreeCache getOrCreateTreeCache(String path) {
    TreeCache cache;

    if (treeCaches.containsKey(path)) {
      cache = treeCaches.get(path);
    } else {
      cache = new TreeCache(client, path);

      treeCaches.put(path, cache);
    }
    return cache;
  }

  public void registerUpdater(ConfigurationUpdater updater) {
    NodeCache cache = getOrCreateNodeCache(updater.getPath());
    if (client.getState().equals(CuratorFrameworkState.STARTED)) {
      startNodeCache(cache);
    }

    cache
        .getListenable()
        .addListener(
            new NodeCacheListener() {
              @Override
              public void nodeChanged() {
                updater.update(cache.getCurrentData().getData());
              }
            });
  }

  public void registerForTreeNodeEvents(String path, Consumer<TreeCacheEvent> updater) {
    TreeCache cache = getOrCreateTreeCache(path);
    if (client.getState().equals(CuratorFrameworkState.STARTED)) {
      startTreeCache(cache);
    }

    cache
      .getListenable()
      .addListener(
        new TreeCacheListener() {
          @Override
          public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
            updater.accept(event);
          }
        });
  }
}
