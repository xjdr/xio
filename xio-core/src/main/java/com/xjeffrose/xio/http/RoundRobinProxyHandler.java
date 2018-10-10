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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import lombok.val;

public class RoundRobinProxyHandler extends ProxyHandler {
  private static final AttributeKey<IntObjectMap<Optional<ClientConfig>>> ROUND_ROBIN_KEY =
      AttributeKey.newInstance("xio_round_robin_key");
  private final AtomicInteger next = new AtomicInteger();
  private final Supplier<Optional<ClientConfig>> computationFunction;

  public static RoundRobinProxyHandler createStatisticalRoundRobinHandler(
      ClientFactory factory, ProxyRouteConfig config, SocketAddressHelper addressHelper) {
    return new RoundRobinProxyHandler(factory, config, addressHelper, ThreadLocalRandom::current);
  }

  public static RoundRobinProxyHandler createStandardRoundRobinHandler(
      ClientFactory factory, ProxyRouteConfig config, SocketAddressHelper addressHelper) {
    return new RoundRobinProxyHandler(factory, config, addressHelper, null);
  }

  private RoundRobinProxyHandler(
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
    if (randomSupplier == null) {
      this.computationFunction = this::computeRoundRobin;
    } else {
      this.computationFunction = () -> this.computeStatisticalRoundRobin(randomSupplier);
    }
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
      return computationFunction.get();
    } else if (request.startOfMessage()) {
      Optional<ClientConfig> newClientConfig = computationFunction.get();
      cachedClientConfig.put(hashKey, newClientConfig);
      return newClientConfig;
    } else if (request.endOfMessage()) {
      return cachedClientConfig.remove(hashKey);
    } else {
      return cachedClientConfig.get(hashKey);
    }
  }

  private Optional<ClientConfig> computeRoundRobin() {
    int idx = next.getAndIncrement();
    // when we overflow from incrementing
    if (idx < 0) {
      next.set(0);
      idx = 0;
    }

    val clientConfigs = config.clientConfigs();
    if (clientConfigs.size() > 0) {
      return Optional.of(clientConfigs.get(idx % clientConfigs.size()));
    }
    return Optional.empty();
  }

  private Optional<ClientConfig> computeStatisticalRoundRobin(Supplier<Random> randomSupplier) {
    List<ClientConfig> clientConfigs = config.clientConfigs();
    if (clientConfigs.size() > 0) {
      int index = randomSupplier.get().nextInt(clientConfigs.size());
      return Optional.of(clientConfigs.get(index));
    } else {
      return Optional.empty();
    }
  }
}
