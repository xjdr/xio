package com.xjeffrose.xio.core;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;

public class ZkClientFunctionalTest extends Assert {

  @Test
  public void testFromExhibitor() throws Exception {
    MockWebServer server = new MockWebServer();
    server.enqueue(
        new MockResponse()
            .setBody(
                "count=5&server0=10.10.1.1&server1=10.10.1.2&server2=10.10.1.3&server3=10.10.1.4&server4=10.10.1.5&port=2181")
            .setHeader("Content-Type", "application/x-www-form-urlencoded"));
    server.start();
    ZkClient client = ZkClient.fromExhibitor(Arrays.asList("127.0.0.1"), server.getPort());

    assertEquals(
        "10.10.1.1:2181,10.10.1.2:2181,10.10.1.3:2181,10.10.1.4:2181,10.10.1.5:2181",
        client.getConnectionString());
    server.shutdown();
  }

  @Test
  public void testUpdaterBeforeStart() throws Exception {
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

        // start the client after updaters have been added
        zkClient.start();

        signal.await();
        assertEquals(payload, result.get());

        zkClient.stop();
      }
    }
  }

  @Test
  public void testUpdaterAfterStart() throws Exception {
    try (TestingServer server = new TestingServer()) {
      server.start();

      String payload = "payload";

      RetryPolicy retryPolicy = new RetryOneTime(1);
      try (CuratorFramework client =
          CuratorFrameworkFactory.newClient(server.getConnectString(), retryPolicy)) {
        client.start();
        String path = "/xio/watched/node-init";

        ZkClient zkClient = new ZkClient(server.getConnectString());
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch signal = new CountDownLatch(1);

        zkClient.start();

        // add new updater after the client has been started
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

        client.create().orSetData().creatingParentsIfNeeded().forPath(path, payload.getBytes());

        signal.await();
        assertEquals(payload, result.get());

        zkClient.stop();
      }
    }
  }

  @Test
  public void testNodeUpdaterUpdate() throws Exception {
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

  @Test
  public void testRegisterForTreeNodeEvents() throws Exception {
    try (TestingServer server = new TestingServer()) {
      server.start();

      String payload1 = "payload-one";
      String payload2 = "payload-two";

      RetryPolicy retryPolicy = new RetryOneTime(1);
      try (CuratorFramework client =
          CuratorFrameworkFactory.newClient(server.getConnectString(), retryPolicy)) {
        client.start();

        String treeNodePath = "/xio/watched/tree-start";

        // add the initial nodes (added parent nodes first)
        client
            .create()
            .orSetData()
            .creatingParentsIfNeeded()
            .forPath(treeNodePath + "/one", payload1.getBytes());

        ZkClient zkClient = new ZkClient(server.getConnectString());

        List<TreeCacheEvent> results = Lists.newArrayList();
        CountDownLatch initializedLatch = new CountDownLatch(1);
        CountDownLatch nodeAddedAfterwardLatch = new CountDownLatch(1);

        // start the client which will notify of all nodes added then followed by an initialized event
        zkClient.start();

        zkClient.registerForTreeNodeEvents(
            treeNodePath,
            treeCacheEvent -> {
              results.add(treeCacheEvent);

              if (treeCacheEvent.getType().equals(TreeCacheEvent.Type.INITIALIZED)) {
                initializedLatch.countDown();
              }

              if (initializedLatch.getCount() == 0
                  && treeCacheEvent.getType().equals(TreeCacheEvent.Type.NODE_ADDED)) {
                nodeAddedAfterwardLatch.countDown();
              }
            });

        // wait for the initial events to finish
        initializedLatch.await();

        // last node should be the initialized node (sanity check)
        TreeCacheEvent initializedEvent = results.get(results.size() - 1);
        assertEquals(TreeCacheEvent.Type.INITIALIZED, initializedEvent.getType());

        // should have stored the first payload
        assertEquals(payload1, zkClient.get(treeNodePath + "/one"));

        // add new child node
        client
            .create()
            .orSetData()
            .creatingParentsIfNeeded()
            .forPath(treeNodePath + "/two", payload2.getBytes());

        // wait for new node to be added
        nodeAddedAfterwardLatch.await();

        // should get an event for the newly added child node
        TreeCacheEvent newEvent = results.get(results.size() - 1);
        assertEquals(TreeCacheEvent.Type.NODE_ADDED, newEvent.getType());
        assertEquals(treeNodePath + "/two", newEvent.getData().getPath());

        // should have stored the second payload
        assertEquals(payload2, zkClient.get(treeNodePath + "/two"));
      }
    }
  }
}
