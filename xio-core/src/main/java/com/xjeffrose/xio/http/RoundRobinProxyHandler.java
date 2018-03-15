package com.xjeffrose.xio.http;

import com.xjeffrose.xio.client.ClientConfig;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.val;

public class RoundRobinProxyHandler extends ProxyHandler {
  private final AtomicInteger next = new AtomicInteger();

  public RoundRobinProxyHandler(ClientFactory factory, ProxyRouteConfig config) {
    super(factory, config);
  }

  @Override
  public ClientConfig getClientConfig(Request request) {
    int idx = next.getAndIncrement();
    val clientConfigs = config.clientConfigs();
    return clientConfigs.get(idx % clientConfigs.size());
  }
}
