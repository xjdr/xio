package com.xjeffrose.xio.zookeeper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;

  public DeploymentCache(CuratorFramework curatorClient, String membershipPath) {
    this.membershipPath = membershipPath;
    this.childrenCache = new PathChildrenCache(curatorClient, membershipPath, true);
    objectMapper = new ObjectMapper();
  }

  public void start() throws Exception {
    childrenCache.start();
    childrenCache.rebuild();
  }

  public void close() throws IOException {
    childrenCache.close();
  }

  public List<DeploymentPayload> getDeployments() throws IOException {
    List<DeploymentPayload> result = new ArrayList<>();
    for (ChildData child : childrenCache.getCurrentData()) {
      byte[] data = child.getData();
      if (data != null) {
        String json = new String(data, StandardCharsets.UTF_8);
        DeploymentPayload payload =
            objectMapper.readValue(json, new TypeReference<DeploymentPayload>() {});
        result.add(payload);
      }
    }

    return result;
  }
}
