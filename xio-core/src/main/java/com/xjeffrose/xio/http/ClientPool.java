package com.xjeffrose.xio.http;

import com.google.common.annotations.VisibleForTesting;
import com.xjeffrose.xio.client.ClientConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientPool {

  private final int maxSizePerAddress;
  private final ConcurrentMap<InetSocketAddress, ConcurrentMap<Client, Meta>> clientPool;

  public ClientPool(int maxSizePerAddress) {
    this.maxSizePerAddress = maxSizePerAddress;
    this.clientPool = PlatformDependent.newConcurrentHashMap();
  }

  private ConcurrentMap<Client, Meta> getPool(InetSocketAddress address) {
    return clientPool.computeIfAbsent(address, k -> PlatformDependent.newConcurrentHashMap());
  }

  public void release(Client client) {
    log.debug("recycling client {}", client);
    client.recycle();
    ConcurrentMap<Client, Meta> pool = getPool(client.remoteAddress());
    if (pool.size() < maxSizePerAddress && !pool.containsKey(client)) {
      log.debug("releasing client to pool {}", client);
      pool.put(client, new Meta(client));
    } else {
      Meta meta = pool.get(client);
      if (meta != null) {
        log.debug("setting client available in pool {}", client);
        meta.available.set(true);
      }
    }
  }

  @VisibleForTesting
  int countAvailable() {
    return clientPool
        .values()
        .stream()
        .filter(map -> !map.isEmpty())
        .flatMap(map -> map.values().stream())
        .filter(meta -> meta.available.get())
        .flatMapToInt(meta -> IntStream.of(1))
        .reduce(0, (i, n) -> i + n);
  }

  public Client acquire(
      ChannelHandlerContext ctx, ClientConfig config, Supplier<Client> clientSupplier) {
    return getPool(config.remote())
        .values()
        .stream()
        .filter(
            meta -> {
              boolean available = meta.available.getAndSet(false);
              if (available) {
                int count = meta.usageCount.incrementAndGet();
                log.debug("reusing client in pool with usage count {}", count);
                meta.client.prepareForReuse(() -> new ProxyBackendHandler(ctx));
              }
              return available;
            })
        .findFirst()
        .map(mta -> mta.client)
        .orElseGet(clientSupplier);
  }

  private static class Meta {
    final Client client;
    AtomicBoolean available = new AtomicBoolean(true);
    AtomicInteger usageCount = new AtomicInteger(1);

    public Meta(Client client) {
      this.client = client;
    }
  }
}
