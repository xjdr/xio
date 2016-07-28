package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Marshaller;
import org.apache.curator.framework.CuratorFramework;

public class ZooKeeperWriteProvider extends WriteProvider {

  private CuratorFramework client;
  private String path;

  public ZooKeeperWriteProvider(Marshaller marshall, CuratorFramework client, String path) {
    super(marshall);
    this.client = client;
    this.path = path;
  }

  public void write(String keyName, byte[] bytes) {
    String key = path + "/" + keyName;
    try {
      client.create().orSetData().creatingParentsIfNeeded().forPath(key, bytes);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
