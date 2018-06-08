package com.xjeffrose.xio.client;

public class IdleTimeoutConfig {
  public final boolean enabled;
  public final int duration;

  public IdleTimeoutConfig(boolean enabled, int duration) {
    this.enabled = enabled;
    this.duration = duration;
  }
}
