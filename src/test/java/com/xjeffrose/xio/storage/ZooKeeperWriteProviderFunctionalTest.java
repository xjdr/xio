package com.xjeffrose.xio.storage;

import com.google.common.collect.HashMultimap;
import com.xjeffrose.xio.config.HostnameDeterministicRuleEngineConfig;
import com.xjeffrose.xio.config.Http1DeterministicRuleEngineConfig;
import com.xjeffrose.xio.config.IpAddressDeterministicRuleEngineConfig;
import com.xjeffrose.xio.marshall.ThriftMarshaller;
import com.xjeffrose.xio.marshall.ThriftUnmarshaller;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import java.net.InetAddress;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Test;

public class ZooKeeperWriteProviderFunctionalTest extends Assert {

  @Test
  public void testWriteHostnameDeterministicRuleEngineConfig() throws Exception {
    try (TestingServer server = new TestingServer()) {
      server.start();

      HostnameDeterministicRuleEngineConfig config = new HostnameDeterministicRuleEngineConfig();

      config.blacklistHost("localhost");
      config.blacklistHost("localhost.localdomain");
      config.whitelistHost("google.com");

      ThriftMarshaller marshaller = new ThriftMarshaller();
      RetryPolicy retryPolicy = new RetryOneTime(1);
      try(CuratorFramework client = CuratorFrameworkFactory.newClient(server.getConnectString(), retryPolicy)) {
        client.start();
        String path = "/some/path/to/nodes/hostRules";

        ZooKeeperWriteProvider provider = new ZooKeeperWriteProvider(marshaller, client);

        provider.write(path, config);

        byte[] data = client.getData().forPath(path);
        ThriftUnmarshaller unmarshaller = new ThriftUnmarshaller();
        HostnameDeterministicRuleEngineConfig read = new HostnameDeterministicRuleEngineConfig();
        unmarshaller.unmarshall(read, data);

        assertEquals(config, read);
      }
    }
  }

  @Test
  public void testWriteHttp1DeterministicRuleEngineConfig() throws Exception {
    try (TestingServer server = new TestingServer()) {
      server.start();

      Http1DeterministicRuleEngineConfig config = new Http1DeterministicRuleEngineConfig();

      HashMultimap<String, String> headers = HashMultimap.create();
      headers.put("User-Agent", "Bad-actor: 1.0");
      Http1DeterministicRuleEngineConfig.Rule bad = new Http1DeterministicRuleEngineConfig.Rule(
        HttpMethod.GET,
        "/path/to/failure",
        HttpVersion.HTTP_1_0,
        headers
      );
      Http1DeterministicRuleEngineConfig.Rule good = new Http1DeterministicRuleEngineConfig.Rule(null, null, null, null);
      config.blacklistRule(bad);
      config.whitelistRule(good);

      ThriftMarshaller marshaller = new ThriftMarshaller();
      RetryPolicy retryPolicy = new RetryOneTime(1);
      try(CuratorFramework client = CuratorFrameworkFactory.newClient(server.getConnectString(), retryPolicy)) {
        client.start();
        String path = "/some/path/to/nodes/http1Rules";

        ZooKeeperWriteProvider provider = new ZooKeeperWriteProvider(marshaller, client);

        provider.write(path, config);

        byte[] data = client.getData().forPath(path);
        ThriftUnmarshaller unmarshaller = new ThriftUnmarshaller();
        Http1DeterministicRuleEngineConfig read = new Http1DeterministicRuleEngineConfig();
        unmarshaller.unmarshall(read, data);

        assertEquals(config, read);
      }
    }
  }

  @Test
  public void testWriteIpAddressDeterministicRuleEngineConfig() throws Exception {
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

        ZooKeeperWriteProvider provider = new ZooKeeperWriteProvider(marshaller, client);

        provider.write(path, config);

        byte[] data = client.getData().forPath(path);
        ThriftUnmarshaller unmarshaller = new ThriftUnmarshaller();
        IpAddressDeterministicRuleEngineConfig read = new IpAddressDeterministicRuleEngineConfig();
        unmarshaller.unmarshall(read, data);

        assertEquals(config, read);
      }
    }
  }

}
