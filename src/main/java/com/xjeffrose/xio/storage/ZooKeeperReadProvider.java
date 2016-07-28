package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Unmarshaller;
import org.apache.curator.framework.CuratorFramework;

public class ZooKeeperReadProvider extends ReadProvider {

  private CuratorFramework client;
  private String path;

  public ZooKeeperReadProvider(Unmarshaller unmarshaller, CuratorFramework client, String path) {
    super(unmarshaller);
    this.client = client;
    this.path = path;
  }

  public byte[] read(String keyName) {
    String key = path + "/" + keyName;
    try {
      return client.getData().forPath(key);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
