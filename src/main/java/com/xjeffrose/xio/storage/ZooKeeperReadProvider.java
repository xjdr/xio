package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.marshall.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

@Slf4j
public class ZooKeeperReadProvider extends ReadProvider {

  private CuratorFramework client;
  private String path;

  public ZooKeeperReadProvider(Unmarshaller unmarshaller, CuratorFramework client, String path) {
    super(unmarshaller);
    this.client = client;
    this.path = path;
  }

  private String key(String keyName) {
    return path + "/" + keyName;
  }

  public boolean exists(String keyName) {
    try {
      return client.checkExists().forPath(key(keyName)) != null;
    } catch (Exception e) {
      log.error("Couldn't determine if node exists for path '{}'", key(keyName), e);
      return false;
    }
  }

  public byte[] read(String keyName) {
    try {
      return client.getData().forPath(key(keyName));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
