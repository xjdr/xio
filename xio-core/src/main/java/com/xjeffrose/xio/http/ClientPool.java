package com.xjeffrose.xio.http;

import com.xjeffrose.xio.client.ClientConfig;
import io.netty.util.internal.ConcurrentSet;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientPool {

  private final int maxSize;
  private final Map<InetSocketAddress, Set<Client>> clientPool;

  public ClientPool(int size) {
    maxSize = size;
    clientPool = new ConcurrentHashMap<>();
  }

  private Set<Client> getPool(InetSocketAddress address) {
    return clientPool.computeIfAbsent(address, k -> new ConcurrentSet<>());
  }

  public void release(Client client) {
    Set<Client> queue = getPool(client.remoteAddresss());
    if (queue.size() < maxSize) {
      log.debug("releasing client to pool {}", client);
      queue.add(client);
    }
  }

  public Client acquire(ClientConfig config, Supplier<Client> clientSupplier) {
    Iterator<Client> iter = getPool(config.remote()).iterator();
    Client client = null;
    if (iter.hasNext()) {
      client = iter.next();
      log.debug("supplying client from pool {}", client);
      iter.remove();
    }
    return Optional.ofNullable(client).orElseGet(clientSupplier);
  }
}
