package com.xjeffrose.xio.core;

import java.util.Iterator;
import java.util.Map;

public interface RequestContext {

  ConnectionContext getConnectionContext();

  void setContextData(String key, Object val);

  Object getContextData(String key);

  void clearContextData(String key);

  Iterator<Map.Entry<String, Object>> contextDataIterator();
}