package com.xjeffrose.xio.zookeeper;

import com.typesafe.config.Config;
import java.util.Arrays;
import java.util.Collection;
import lombok.Getter;
import okhttp3.HttpUrl;

@Getter
public class ExhibitorConfig {
  private final HttpUrl url;
  private final int pollingMs;
  private final RetryConfig retryConfig;

  public ExhibitorConfig(Config config) {
    this.url = HttpUrl.parse(config.getString("url"));
    this.pollingMs = (int) config.getDuration("polling").toMillis();
    this.retryConfig = new RetryConfig(config.getConfig("retry"));
  }

  public Collection<String> getHostnames() {
    return Arrays.asList(url.host());
  }

  public int getRestPort() {
    return url.port();
  }

  public String getRestUriPath() {
    return url.encodedPath();
  }
}
