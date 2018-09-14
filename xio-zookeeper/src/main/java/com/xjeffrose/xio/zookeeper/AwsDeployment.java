package com.xjeffrose.xio.zookeeper;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.Value;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.curator.ensemble.exhibitor.DefaultExhibitorRestClient;
import org.apache.curator.ensemble.exhibitor.ExhibitorEnsembleProvider;
import org.apache.curator.ensemble.exhibitor.Exhibitors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.nodes.GroupMember;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class AwsDeployment {
  private final AwsDeploymentConfig config;
  private final CuratorFramework curatorClient;
  private final GroupMember groupMember;

  private RetryPolicy buildRetryPolicy(RetryConfig config) {
    return new ExponentialBackoffRetry(config.getBaseSleepTimeMs(), config.getMaxRetries());
  }

  private EnsembleProvider buildEnsembleProvider(ExhibitorConfig config) throws Exception {
    Exhibitors exhibitors = new Exhibitors(config.getHostnames(), config.getRestPort(), () -> "");
    RetryPolicy retryPolicy = buildRetryPolicy(config.getRetryConfig());
    ExhibitorEnsembleProvider ensembleProvider =
        new ExhibitorEnsembleProvider(
            exhibitors,
            new DefaultExhibitorRestClient(),
            config.getRestUriPath(),
            config.getPollingMs(),
            retryPolicy);
    ensembleProvider.pollForInitialEnsemble();
    return ensembleProvider;
  }

  private CuratorFramework buildCuratorClient(
      EnsembleProvider ensembleProvider, ZookeeperConfig config) throws Exception {
    RetryPolicy retryPolicy = buildRetryPolicy(config.getRetryConfig());
    CuratorFramework curatorClient =
        CuratorFrameworkFactory.builder()
            .ensembleProvider(ensembleProvider)
            .retryPolicy(retryPolicy)
            .build();
    return curatorClient;
  }

  static class Identity {
    String availabilityZone;
    String instanceId;
    String privateIp;
    String region;
  }

  @Value
  static class Payload {
    String host;
    int port;
  }

  private Identity getIdentity(AwsDeploymentConfig config) throws IOException {
    OkHttpClient client = new OkHttpClient();
    Moshi moshi = new Moshi.Builder().build();
    JsonAdapter<Identity> identityJsonAdapter = moshi.adapter(Identity.class);
    Request request = new Request.Builder().url(config.getIdentityUrl()).build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

      Identity identity = identityJsonAdapter.fromJson(response.body().source());
      return identity;
    }
  }

  private GroupMember buildGroupMember(
      CuratorFramework curatorClient, ZookeeperConfig config, String instanceId, Payload payload) {
    Moshi moshi = new Moshi.Builder().build();
    JsonAdapter<Payload> payloadJsonAdapter = moshi.adapter(Payload.class);
    byte[] payloadBytes = payloadJsonAdapter.toJson(payload).getBytes(StandardCharsets.UTF_8);

    return new GroupMember(curatorClient, config.getMembershipPath(), instanceId, payloadBytes);
  }

  public AwsDeployment(AwsDeploymentConfig config, int port) throws Exception {
    this.config = config;
    EnsembleProvider ensembleProvider = buildEnsembleProvider(config.getExhibitorConfig());
    this.curatorClient = buildCuratorClient(ensembleProvider, config.getZookeeperConfig());
    Identity identity = getIdentity(config);
    Payload payload = new Payload(identity.privateIp, port);
    this.groupMember =
        buildGroupMember(curatorClient, config.getZookeeperConfig(), identity.instanceId, payload);
  }

  public void start() throws InterruptedException {
    curatorClient.start();
    curatorClient.blockUntilConnected();
    groupMember.start();
  }

  public void close() {
    groupMember.close();
    curatorClient.close();
  }
}
