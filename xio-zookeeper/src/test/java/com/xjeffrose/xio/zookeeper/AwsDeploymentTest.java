package com.xjeffrose.xio.zookeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AwsDeploymentTest {
  private TestingServer zookeeper;
  private MockWebServer exhibitor;
  private MockWebServer metaDataService;
  private AwsDeploymentConfig awsDeploymentConfig;
  private AwsDeployment awsDeployment;

  private MockWebServer buildMetaDataService() {
    MockWebServer server = new MockWebServer();
    MockResponse response =
        new MockResponse()
            .setBody(
                String.join(
                    "\n",
                    "{",
                    "    \"devpayProductCodes\" : null,",
                    "    \"marketplaceProductCodes\" : [ \"1abc2defghijklm3nopqrs4tu\" ], ",
                    "    \"availabilityZone\" : \"us-west-2b\",",
                    "    \"privateIp\" : \"10.158.112.84\",",
                    "    \"version\" : \"2017-09-30\",",
                    "    \"instanceId\" : \"i-1234567890abcdef0\",",
                    "    \"billingProducts\" : null,",
                    "    \"instanceType\" : \"t2.micro\",",
                    "    \"accountId\" : \"123456789012\",",
                    "    \"imageId\" : \"ami-5fb8c835\",",
                    "    \"pendingTime\" : \"2016-11-19T16:32:11Z\",",
                    "    \"architecture\" : \"x86_64\",",
                    "    \"kernelId\" : null,",
                    "    \"ramdiskId\" : null,",
                    "    \"region\" : \"us-west-2\"",
                    "}"))
            .setResponseCode(200);
    server.enqueue(response);

    return server;
  }

  private MockWebServer buildExhibitor(int port) {
    MockWebServer server = new MockWebServer();
    MockResponse response =
        new MockResponse()
            .setBody("count=1&server0=127.0.0.1&port=" + port)
            .setHeader("Content-Type", "application/x-www-form-urlencoded")
            .setResponseCode(200);
    server.enqueue(response);

    return server;
  }

  @Before
  public void beforeEach() throws Exception {
    zookeeper = new TestingServer(true);
    exhibitor = buildExhibitor(zookeeper.getPort());
    exhibitor.start();
    metaDataService = buildMetaDataService();
    metaDataService.start();

    Config config =
        ConfigFactory.load("aws-deployment-test")
            .withValue(
                "awsDeploymentTest.exhibitor.url",
                ConfigValueFactory.fromAnyRef(exhibitor.url("/").toString()))
            .withValue(
                "awsDeploymentTest.identityUrl",
                ConfigValueFactory.fromAnyRef(metaDataService.url("/").toString()));

    awsDeploymentConfig = new AwsDeploymentConfig(config.getConfig("awsDeploymentTest"));
    awsDeployment = new AwsDeployment(awsDeploymentConfig, 443);
  }

  @After
  public void afterEach() throws Exception {
    awsDeployment.close();
    zookeeper.close();
    exhibitor.close();
    metaDataService.close();
  }

  @Test
  public void testDeployment() throws Exception {
    awsDeployment.start();

    RetryPolicy retryPolicy = new RetryOneTime(1);
    try (CuratorFramework client =
        CuratorFrameworkFactory.newClient(zookeeper.getConnectString(), retryPolicy)) {
      client.start();
      client.blockUntilConnected();
      String path = "/test-path/i-1234567890abcdef0";

      assertNotNull(client.checkExists().forPath(path));
      String data = new String(client.getData().forPath(path));

      Moshi moshi = new Moshi.Builder().build();
      JsonAdapter<AwsDeployment.Payload> payloadJsonAdapter =
          moshi.adapter(AwsDeployment.Payload.class);
      AwsDeployment.Payload payload = payloadJsonAdapter.fromJson(data);
      assertEquals("10.158.112.84", payload.getHost());
      assertEquals(443, payload.getPort());
    }
  }
}
