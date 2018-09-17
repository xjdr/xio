package com.xjeffrose.xio.zookeeper;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;

public class DeploymentCache {
  @Getter private final String membershipPath;
  private final PathChildrenCache childrenCache;
  private final Moshi moshi;
  private final JsonAdapter<DeploymentPayload> payloadJsonAdapter;

  public DeploymentCache(CuratorFramework curatorClient, String membershipPath) throws Exception {
    this.membershipPath = membershipPath;
    this.childrenCache = new PathChildrenCache(curatorClient, membershipPath, true);
    this.moshi = new Moshi.Builder().build();
    this.payloadJsonAdapter = moshi.adapter(DeploymentPayload.class);
  }

  public void start() throws Exception {
    childrenCache.start();
    childrenCache.rebuild();
  }

  public void close() throws IOException {
    childrenCache.close();
  }

  public List<DeploymentPayload> getDeployments() throws IOException {
    List<DeploymentPayload> result = new ArrayList<DeploymentPayload>();
    for (ChildData child : childrenCache.getCurrentData()) {
      byte[] data = child.getData();
      if (data != null) {
        String json = new String(data, StandardCharsets.UTF_8);
        DeploymentPayload payload = payloadJsonAdapter.fromJson(json);
        result.add(payload);
      }
    }

    return result;
  }
}
