package com.xjeffrose.xio.config;

import com.typesafe.config.Config;

public class TracingConfig {
  private final String applicationName;
  private final String zipkinUrl;
  private final float zipkinSamplingRate;

  public TracingConfig(String applicationName, Config config) {
    this.applicationName = applicationName;
    this.zipkinUrl = config.getString("zipkinUrl");
    this.zipkinSamplingRate = (float) config.getDouble("samplingRate");
  }

  public TracingConfig(String applicationName, String zipkinUrl, float zipkinSamplingRate) {
    this.applicationName = applicationName;
    this.zipkinUrl = zipkinUrl;
    this.zipkinSamplingRate = zipkinSamplingRate;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public String getZipkinUrl() {
    return zipkinUrl;
  }

  public float getZipkinSamplingRate() {
    return zipkinSamplingRate;
  }
}
