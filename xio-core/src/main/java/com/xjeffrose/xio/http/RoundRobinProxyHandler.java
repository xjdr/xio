package com.xjeffrose.xio.http;

import com.google.common.annotations.VisibleForTesting;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.SocketAddressHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.internal.ThreadLocalRandom;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

public class RoundRobinProxyHandler extends ProxyHandler {
  private static final AttributeKey<IntObjectMap<Optional<ClientConfig>>> ROUND_ROBIN_KEY =
      AttributeKey.newInstance("xio_round_robin_key");
  private final Supplier<Random> randomSupplier;

  public RoundRobinProxyHandler(
      ClientFactory factory, ProxyRouteConfig config, SocketAddressHelper addressHelper) {
    this(factory, config, addressHelper, ThreadLocalRandom::current);
  }

  @VisibleForTesting
  RoundRobinProxyHandler(
      ClientFactory factory,
      ProxyRouteConfig config,
      SocketAddressHelper addressHelper,
      Supplier<Random> randomSupplier) {
    super(factory, config, addressHelper);
    this.randomSupplier = randomSupplier;
  }

  @Override
  public Optional<ClientConfig> getClientConfig(ChannelHandlerContext ctx, Request request) {
    // get/create the cachedClientConfig that is instanced per server channel
    IntObjectMap<Optional<ClientConfig>> cachedClientConfig =
        ctx.channel().attr(ROUND_ROBIN_KEY).get();
    if (cachedClientConfig == null) {
      cachedClientConfig = new IntObjectHashMap<>();
      ctx.channel().attr(ROUND_ROBIN_KEY).set(cachedClientConfig);
    }
    return getClientConfig(cachedClientConfig, request);
  }

  @VisibleForTesting
  Optional<ClientConfig> getClientConfig(
      IntObjectMap<Optional<ClientConfig>> cachedClientConfig, Request request) {
    int hashKey = request.streamId();
    // we only do a fresh round robin if the request is a either a FULL HTTP REQUEST or the First Part of a Chunked Requets
    if (request.isFullMessage()) {
      return computeStatisticalRoundRobin();
    } else if (request.startOfMessage()) {
      Optional<ClientConfig> newClientConfig = computeStatisticalRoundRobin();
      cachedClientConfig.put(hashKey, newClientConfig);
      return newClientConfig;
    } else if (request.endOfMessage()) {
      return cachedClientConfig.remove(hashKey);
    } else {
      return cachedClientConfig.get(hashKey);
    }
  }

  private Optional<ClientConfig> computeStatisticalRoundRobin() {
    List<ClientConfig> clientConfigs = config.clientConfigs();
    if (clientConfigs.size() > 0) {
      int index = randomSupplier.get().nextInt(clientConfigs.size());
      return Optional.of(clientConfigs.get(index));
    } else {
      return Optional.empty();
    }
  }
}
