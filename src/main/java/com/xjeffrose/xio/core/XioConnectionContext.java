package com.xjeffrose.xio.core;


import com.google.common.base.Preconditions;
import com.xjeffrose.xio.core.ConnectionContext;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XioConnectionContext implements ConnectionContext {
  private SocketAddress remoteAddress;
  private Map<String, Object> attributes = new ConcurrentHashMap<>();

  @Override
  public SocketAddress getRemoteAddress() {
    return remoteAddress;
  }

  public void setRemoteAddress(SocketAddress remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  @Override
  public Object getAttribute(String attributeName) {
    Preconditions.checkNotNull(attributeName);
    return attributes.get(attributeName);
  }

  @Override
  public Object setAttribute(String attributeName, Object value) {
    Preconditions.checkNotNull(attributeName);
    Preconditions.checkNotNull(value);
    return attributes.put(attributeName, value);
  }

  @Override
  public Object removeAttribute(String attributeName) {
    Preconditions.checkNotNull(attributeName);
    return attributes.remove(attributeName);
  }

  @Override
  public Iterator<Map.Entry<String, Object>> attributeIterator() {
    return Collections.unmodifiableSet(attributes.entrySet()).iterator();
  }
}
