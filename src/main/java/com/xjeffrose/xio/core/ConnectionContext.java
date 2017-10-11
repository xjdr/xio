package com.xjeffrose.xio.core;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;

public interface ConnectionContext {

  public SocketAddress getRemoteAddress();

  public Object getAttribute(String attributeName);

  public Object setAttribute(String attributeName, Object value);

  public Object removeAttribute(String attributeName);

  public Iterator<Map.Entry<String, Object>> attributeIterator();
}