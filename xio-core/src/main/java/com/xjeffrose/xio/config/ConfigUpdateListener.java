package com.xjeffrose.xio.config;

/** WIP potential interface for config updates */
public interface ConfigUpdateListener<T> {
  void notifyChanged(T oldValue, T newValue);

  boolean commit();

  void rollback();
}
