package com.xjeffrose.xio.core;

import java.net.InetSocketAddress;
import java.util.List;

public interface ConfigurationProvider {

  // events (registration, deregistration)
  // node added, node removed
  // client added, client removed
  // leader election
  // counters
  // locks
  // timestamps

  public void register(String path, InetSocketAddress address);

  public void electLeader(String path);

  public void set(String path, String data);

  public String get(String path);

  public List<String> list(String path);

  public List<String> getChildren(String path);

  public void registerUpdater(ConfigurationUpdater updater);
}
