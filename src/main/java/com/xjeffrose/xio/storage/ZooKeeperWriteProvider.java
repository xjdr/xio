package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

@Slf4j
public class ZooKeeperWriteProvider extends WriteProvider {

  private CuratorFramework client;

  public ZooKeeperWriteProvider(Marshaller marshall, CuratorFramework client) {
    super(marshall);
    this.client = client;
  }

  public void write(String key, byte[] bytes) {
    try {
      client.create().orSetData().creatingParentsIfNeeded().forPath(key, bytes);
    } catch (Exception e) {
      log.error("ZooKeeperWriteProvider.write", e);
      throw new RuntimeException(e);
    }
  }
}
