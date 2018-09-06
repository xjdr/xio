package com.xjeffrose.xio.config;

import com.typesafe.config.Config;

public class TracingConfig {
  public enum TracingType {
    ZIPKIN,
    DATADOG
  }

  private final String applicationName;
  private String zipkinUrl;
  private float zipkinSamplingRate;
  private final TracingType type;

  public TracingConfig(String applicationName, Config config) {
    this.applicationName = applicationName;

    TracingType type;
    if (config.hasPath("type")) {
      type = config.getEnum(TracingType.class, "type");
    } else {
      type = TracingType.ZIPKIN;
    }
    this.type = type;

    if (type == TracingType.ZIPKIN) {
      String zipkinUrl;
      if (config.hasPath("zipkin.zipkinUrl")) {
        zipkinUrl = config.getString("zipkin.zipkinUrl");
      } else {
        zipkinUrl = "";
      }

      float samplingRate;
      if (config.hasPath("zipkin.samplingRate")) {
        samplingRate = ((Double) config.getDouble("zipkin.samplingRate")).floatValue();
      } else {
        samplingRate = 0.01f;
      }

      this.zipkinUrl = zipkinUrl;
      this.zipkinSamplingRate = samplingRate;
    }
  }

  public TracingConfig(
      String applicationName, String zipkinUrl, float zipkinSamplingRate, TracingType type) {
    this.applicationName = applicationName;
    this.zipkinUrl = zipkinUrl;
    this.zipkinSamplingRate = zipkinSamplingRate;
    this.type = type;
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

  public TracingType getType() {
    return type;
  }
}
