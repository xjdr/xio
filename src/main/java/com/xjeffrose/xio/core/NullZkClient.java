package com.xjeffrose.xio.core;

import java.net.InetSocketAddress;
import java.util.List;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;

@Slf4j
public class NullZkClient extends ZkClient {

  public NullZkClient() {
    log.warn("Building NullZkClient, runtime configuration will be disabled");
  }

  @Override
  public void rebuild() {}

  @Override
  public void register(String NODE_LIST_PATH, InetSocketAddress address, @Nullable byte[] data) {}

  @Override
  public void register(String NODE_LIST_PATH, String ip, int port, @Nullable byte[] data) {}

  @Override
  public void register(String path, InetSocketAddress address) {}

  @Override
  public void electLeader(String ELECTION_PATH, LeaderSelectorListener listener) {}

  @Override
  public void electLeader(String path) {}

  @Override
  public void startNodeCache(NodeCache cache) {}

  @Override
  public void start() {}

  @Override
  public void stopNodeCache(NodeCache cache) {}

  @Override
  public void stop() throws Exception {}

  @Override
  public void set(String path, String data) {}

  @Override
  public String get(String path) {
    return null;
  }

  @Override
  public void set(String path, String data, boolean compress) {}

  @Override
  public String get(String path, boolean compress) {
    return null;
  }

  @Override
  public List<String> list(String path) {
    return null;
  }

  @Override
  public List<String> getChildren(String path) {
    return null;
  }

  @Override
  public CuratorFramework getClient() {
    return null;
  }

  @Override
  public String getConnectionString() {
    return "";
  }

  @Override
  public void registerUpdater(ConfigurationUpdater updater) {}
}
