package com.xjeffrose.xio.core;


import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class XioRequestContext implements RequestContext {
  private final ConnectionContext connectionContext;
  private final ConcurrentMap<String, Object> data = Maps.newConcurrentMap();

  public XioRequestContext(ConnectionContext connectionContext) {
    this.connectionContext = connectionContext;
  }

  @Override
  public ConnectionContext getConnectionContext() {
    return connectionContext;
  }

  @Override
  public void setContextData(String key, Object val) {
    checkNotNull(key, "context data key is null");
    data.put(key, val);
  }

  @Override
  public Object getContextData(String key) {
    checkNotNull(key, "context data key is null");
    return data.get(key);
  }

  @Override
  public void clearContextData(String key) {
    checkNotNull(key, "context data key is null");
    data.remove(key);
  }

  @Override
  public Iterator<Map.Entry<String, Object>> contextDataIterator() {
    return Collections.unmodifiableSet(data.entrySet()).iterator();
  }
}