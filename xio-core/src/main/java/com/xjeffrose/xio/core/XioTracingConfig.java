package com.xjeffrose.xio.core;

public interface XioTracingConfig {
  public String getName();
  public String getZipkinUrl();
  public float getSamplingRate();
}
