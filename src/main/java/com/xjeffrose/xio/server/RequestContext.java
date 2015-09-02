package com.xjeffrose.xio.server;

import com.xjeffrose.xio.core.ConnectionContext;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public interface RequestContext {

  ConnectionContext getConnectionContext();

  void setContextData(UUID key, Object val);

  Object getContextData(UUID key);

  void clearContextData(UUID key);

  Iterator<Map.Entry<UUID, Object>> contextDataIterator();

  UUID getConnectionId();
}