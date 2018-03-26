package com.xjeffrose.xio.http;

import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.tracing.XioTracing;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.val;

public class RoundRobinProxyHandler extends ProxyHandler {
  private final AtomicInteger next = new AtomicInteger();

  public RoundRobinProxyHandler(
      ClientFactory factory,
      ProxyRouteConfig config,
      SocketAddressHelper addressHelper,
      XioTracing tracing) {
    super(factory, config, addressHelper, tracing);
  }

  @Override
  public ClientConfig getClientConfig(Request request) {
    int idx = next.getAndIncrement();
    val clientConfigs = config.clientConfigs();
    return clientConfigs.get(idx % clientConfigs.size());
  }
}
