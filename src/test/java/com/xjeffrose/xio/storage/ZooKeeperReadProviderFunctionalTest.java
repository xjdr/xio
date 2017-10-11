package com.xjeffrose.xio.storage;

import com.xjeffrose.xio.config.IpAddressDeterministicRuleEngineConfig;
import com.xjeffrose.xio.marshall.ThriftMarshaller;
import com.xjeffrose.xio.marshall.ThriftUnmarshaller;
import java.net.InetAddress;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Test;

public class ZooKeeperReadProviderFunctionalTest extends Assert {

  @Test
  public void testReadIpAddressDeterministicRuleEngineConfig() throws Exception {
    try (TestingServer server = new TestingServer()) {
      server.start();

      IpAddressDeterministicRuleEngineConfig config = new IpAddressDeterministicRuleEngineConfig();

      config.blacklistIp(InetAddress.getByName("127.0.0.1"));
      config.blacklistIp(InetAddress.getByName("::1"));
      config.whitelistIp(InetAddress.getByName("0.0.0.0"));

      ThriftMarshaller marshaller = new ThriftMarshaller();
      RetryPolicy retryPolicy = new RetryOneTime(1);
      try(CuratorFramework client = CuratorFrameworkFactory.newClient(server.getConnectString(), retryPolicy)) {
        client.start();
        String path = "/some/path/to/nodes/ipRules";

        byte[] bytes = marshaller.marshall(config);
        client.create().orSetData().creatingParentsIfNeeded().forPath(path, bytes);

        ThriftUnmarshaller unmarshaller = new ThriftUnmarshaller();
        ZooKeeperReadProvider provider = new ZooKeeperReadProvider(unmarshaller, client);

        IpAddressDeterministicRuleEngineConfig read = new IpAddressDeterministicRuleEngineConfig();
        provider.read(path, read);

        assertEquals(config, read);
      }
    }
  }

}
