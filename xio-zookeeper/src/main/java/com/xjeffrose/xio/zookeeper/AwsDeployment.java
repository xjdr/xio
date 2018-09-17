package com.xjeffrose.xio.zookeeper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
  private final ObjectMapper objectMapper;
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Identity {
    @JsonProperty("availabilityZone")
    String availabilityZone;

    @JsonProperty("instanceId")
    String instanceId;

    @JsonProperty("privateIp")
    String privateIp;

    @JsonProperty("region")
    String region;
  }

  private Identity getIdentity(AwsDeploymentConfig config)
      throws IOException, JsonProcessingException {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(config.getIdentityUrl()).build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

      Identity identity =
          objectMapper.readValue(response.body().string(), new TypeReference<Identity>() {});
      return identity;
    }
  }

  private GroupMember buildGroupMember(
      CuratorFramework curatorClient,
      ZookeeperConfig config,
      String instanceId,
      DeploymentPayload payload)
      throws JsonProcessingException {

    byte[] payloadBytes = objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);

    return new GroupMember(curatorClient, config.getMembershipPath(), instanceId, payloadBytes);
  }

  public AwsDeployment(AwsDeploymentConfig config, int port) throws Exception {
    this.config = config;
    objectMapper = new ObjectMapper();
    EnsembleProvider ensembleProvider = buildEnsembleProvider(config.getExhibitorConfig());
    this.curatorClient = buildCuratorClient(ensembleProvider, config.getZookeeperConfig());
    Identity identity = getIdentity(config);
    DeploymentPayload payload = new DeploymentPayload(identity.privateIp, port);
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
