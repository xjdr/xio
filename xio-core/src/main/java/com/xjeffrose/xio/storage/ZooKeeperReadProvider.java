package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

@Slf4j
public class ZooKeeperReadProvider extends ReadProvider {

  private CuratorFramework client;

  public ZooKeeperReadProvider(Unmarshaller unmarshaller, CuratorFramework client) {
    super(unmarshaller);
    this.client = client;
  }

  public boolean exists(String key) {
    try {
      return client.checkExists().forPath(key) != null;
    } catch (Exception e) {
      log.error("Couldn't determine if node exists for path '{}'", key, e);
      return false;
    }
  }

  public byte[] read(String key) {
    try {
      return client.getData().forPath(key);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
