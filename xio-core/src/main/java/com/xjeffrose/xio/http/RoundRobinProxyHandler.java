package com.xjeffrose.xio.http;

import com.google.common.annotations.VisibleForTesting;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.SocketAddressHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.val;

public class RoundRobinProxyHandler extends ProxyHandler {
  private static final AttributeKey<Map<String, Optional<ClientConfig>>> ROUND_ROBIN_KEY =
      AttributeKey.newInstance("xio_round_robin_key");
  private final AtomicInteger next = new AtomicInteger();

  public RoundRobinProxyHandler(
      ClientFactory factory, ProxyRouteConfig config, SocketAddressHelper addressHelper) {
    super(factory, config, addressHelper);
  }

  @Override
  public Optional<ClientConfig> getClientConfig(ChannelHandlerContext ctx, Request request) {
    // get/create the cachedClientConfig that is instanced per server channel
    Map<String, Optional<ClientConfig>> cachedClientConfig =
        ctx.channel().attr(ROUND_ROBIN_KEY).get();
    if (cachedClientConfig == null) {
      cachedClientConfig = new HashMap<>();
      ctx.channel().attr(ROUND_ROBIN_KEY).set(cachedClientConfig);
    }
    return getClientConfig(cachedClientConfig, request);
  }

  @VisibleForTesting
  Optional<ClientConfig> getClientConfig(
      Map<String, Optional<ClientConfig>> cachedClientConfig, Request request) {
    // build hash key based on the request path + stream id
    String hashKey = request.path() + ":" + request.streamId();

    // we only do a fresh round robin if the request is the start of message
    // full httprequest or httprequest (when chunked)
    if (request.isFullMessage()) {
      return computeRoundRobin(request);
    } else if (request.startOfMessage()) {
      Optional<ClientConfig> newClientConfig = computeRoundRobin(request);
      cachedClientConfig.put(hashKey, newClientConfig);
      return newClientConfig;
    } else if (request.endOfMessage()) {
      return cachedClientConfig.remove(hashKey);
    } else {
      return cachedClientConfig.get(hashKey);
    }
  }

  private Optional<ClientConfig> computeRoundRobin(Request request) {
    int idx = next.getAndIncrement();
    val clientConfigs = config.clientConfigs();
    if (clientConfigs.size() > 0) {
      return Optional.of(clientConfigs.get(idx % clientConfigs.size()));
    }
    return Optional.empty();
  }
}
