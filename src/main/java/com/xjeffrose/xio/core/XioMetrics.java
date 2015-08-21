package com.xjeffrose.xio.core;

public interface XioMetrics {
  int getChannelCount();

  long getBytesRead();

  long getBytesWritten();
}
