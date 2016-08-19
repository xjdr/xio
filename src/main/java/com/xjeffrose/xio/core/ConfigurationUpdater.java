package com.xjeffrose.xio.core;

public interface ConfigurationUpdater {

  String getPath();

  void update(byte[] data);

}
