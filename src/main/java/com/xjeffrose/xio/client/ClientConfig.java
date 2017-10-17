package com.xjeffrose.xio.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.netty.channel.ChannelOption;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientConfig {
  @Getter
  private final Map<ChannelOption<Object>, Object> bootstrapOptions;
  @Getter
  private String name;
  @Getter
  private TlsConfig tls;

  public ClientConfig(Config config) {
    bootstrapOptions = null;
    name = config.getString("name");
    tls = new TlsConfig(config.getConfig("settings.tls"));
    if (!tls.isUseSsl() && tls.isLogInsecureConfig()) {
      log.warn("Client '{}' has useSsl set to false!", name);
    }
  }

  static public ClientConfig fromConfig(String key, Config config) {
    return new ClientConfig(config.getConfig(key));
  }

  static public ClientConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

}
