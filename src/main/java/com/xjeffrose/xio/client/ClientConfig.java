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
  @Getter private final Map<ChannelOption<Object>, Object> bootstrapOptions;
  @Getter private final String name;
  @Getter private final TlsConfig tls;
  @Getter private final boolean messageLoggerEnabled;

  public ClientConfig(Config config) {
    bootstrapOptions = null;
    name = config.getString("name");
    tls = new TlsConfig(config.getConfig("settings.tls"));
    if (!tls.isUseSsl() && tls.isLogInsecureConfig()) {
      log.warn("Client '{}' has useSsl set to false!", name);
    }
    messageLoggerEnabled = config.getBoolean("settings.messageLoggerEnabled");
  }

  public static ClientConfig fromConfig(String key, Config config) {
    return new ClientConfig(config.getConfig(key));
  }

  public static ClientConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }
}
