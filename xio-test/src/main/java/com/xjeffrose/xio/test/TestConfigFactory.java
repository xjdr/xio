package com.xjeffrose.xio.test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigResolveOptions;

public class TestConfigFactory {

  public static Config load(Config overrides) {
    Config defaultOverrides = ConfigFactory.defaultOverrides();
    Config application = ConfigFactory.defaultApplication();
    Config reference = ConfigFactory.defaultReference();
    ConfigResolveOptions resolveOptions = ConfigResolveOptions.defaults();
    return overrides
        .withFallback(defaultOverrides)
        .withFallback(application)
        .withFallback(reference)
        .resolve(resolveOptions);
  }
}
