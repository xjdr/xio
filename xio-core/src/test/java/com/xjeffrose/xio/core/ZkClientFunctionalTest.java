package com.xjeffrose.xio.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Test;

public class ZkClientFunctionalTest extends Assert {

  @Test
  public void testUpdaterInit() throws Exception {
    try (TestingServer server = new TestingServer()) {
      server.start();

      String payload = "payload";

      RetryPolicy retryPolicy = new RetryOneTime(1);
      try (CuratorFramework client =
          CuratorFrameworkFactory.newClient(server.getConnectString(), retryPolicy)) {
        client.start();
        String path = "/xio/watched/node-init";

        client.create().orSetData().creatingParentsIfNeeded().forPath(path, payload.getBytes());

        ZkClient zkClient = new ZkClient(server.getConnectString());
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch signal = new CountDownLatch(1);

        zkClient.registerUpdater(
            new ConfigurationUpdater() {
              @Override
              public String getPath() {
                return path;
              }

              @Override
              public void update(byte[] data) {
                result.set(new String(data));
                signal.countDown();
              }
            });

        zkClient.start();

        signal.await();
        assertEquals(payload, result.get());

        zkClient.stop();
      }
    }
  }

  @Test
  public void testUpdaterUpdate() throws Exception {
    try (TestingServer server = new TestingServer()) {
      server.start();

      String payload = "payload";

      RetryPolicy retryPolicy = new RetryOneTime(1);
      try (CuratorFramework client =
          CuratorFrameworkFactory.newClient(server.getConnectString(), retryPolicy)) {
        client.start();
        String path = "/xio/watched/node-update";

        client.create().orSetData().creatingParentsIfNeeded().forPath(path, payload.getBytes());

        ZkClient zkClient = new ZkClient(server.getConnectString());
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch firstSignal = new CountDownLatch(1);
        CountDownLatch secondSignal = new CountDownLatch(2);

        zkClient.registerUpdater(
            new ConfigurationUpdater() {
              @Override
              public String getPath() {
                return path;
              }

              @Override
              public void update(byte[] data) {
                result.set(new String(data));
                firstSignal.countDown();
                secondSignal.countDown();
              }
            });

        zkClient.start();

        firstSignal.await();
        assertEquals(payload, result.get());

        String update = "updated-payload";

        client.create().orSetData().creatingParentsIfNeeded().forPath(path, update.getBytes());

        secondSignal.await();
        assertEquals(update, result.get());

        zkClient.stop();
      }
    }
  }
}
