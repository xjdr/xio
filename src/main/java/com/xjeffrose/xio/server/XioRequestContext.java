package com.xjeffrose.xio.server;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Maps;
import com.xjeffrose.xio.core.ConnectionContext;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class XioRequestContext implements RequestContext {
  private final ConnectionContext connectionContext;
  private final ConcurrentMap<UUID, Object> data = Maps.newConcurrentMap();
  private final UUID connectionId = genID();

  public XioRequestContext(ConnectionContext connectionContext) {
    this.connectionContext = connectionContext;
  }

  private UUID genID() {
    return UUID.randomUUID();
  }

  @Override
  public ConnectionContext getConnectionContext() {
    return connectionContext;
  }

  @Override
  public void setContextData(UUID key, Object val) {
    checkNotNull(key, "context data key is null");
    data.put(key, val);
  }

  @Override
  public Object getContextData(UUID key) {
    checkNotNull(key, "context data key is null");
    return data.get(key);
  }

  @Override
  public void clearContextData(UUID key) {
    checkNotNull(key, "context data key is null");
    data.remove(key);
  }

  @Override
  public Iterator<Map.Entry<UUID, Object>> contextDataIterator() {
    return Collections.unmodifiableSet(data.entrySet()).iterator();
  }

  @Override
  public UUID getConnectionId() {
    return connectionId;
  }
}