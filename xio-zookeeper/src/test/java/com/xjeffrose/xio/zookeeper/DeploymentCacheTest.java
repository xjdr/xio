package com.xjeffrose.xio.zookeeper;

import static org.junit.Assert.assertEquals;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.nodes.GroupMember;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DeploymentCacheTest {
  private TestingServer zookeeper;
  private CuratorFramework cacheClient;
  private DeploymentCache deploymentCache;

  @Before
  public void beforeEach() throws Exception {
    zookeeper = new TestingServer(true);

    RetryPolicy retryPolicy = new RetryOneTime(1);
    cacheClient = CuratorFrameworkFactory.newClient(zookeeper.getConnectString(), retryPolicy);
    cacheClient.start();
    cacheClient.blockUntilConnected();
    deploymentCache = new DeploymentCache(cacheClient, "/member");
  }

  @After
  public void afterEach() throws Exception {
    deploymentCache.close();
    cacheClient.close();
    zookeeper.close();
  }

  @Test
  public void testCache() throws Exception {
    deploymentCache.start();
    GroupMember groupMember1 = null;
    GroupMember groupMember2 = null;
    CuratorFramework client =
        CuratorFrameworkFactory.newClient(zookeeper.getConnectString(), new RetryOneTime(1));
    try {
      client.start();

      groupMember1 =
          new GroupMember(
              client,
              "/member",
              "1",
              "{\"host\" : \"10.10.10.1\", \"port\" : 443}".getBytes("UTF-8"));
      Assert.assertTrue(groupMember1.getCurrentMembers().containsKey("1"));
      groupMember1.start();

      groupMember2 =
          new GroupMember(
              client,
              "/member",
              "2",
              "{\"host\" : \"10.10.10.2\", \"port\" : 443}".getBytes("UTF-8"));
      groupMember2.start();

      Thread.sleep(2000);
      assertEquals(2, deploymentCache.getDeployments().size());
    } finally {
      groupMember1.close();
      groupMember2.close();
      client.close();
    }
  }
}
