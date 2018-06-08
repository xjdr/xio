package com.xjeffrose.xio.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.netty.channel.ChannelOption;
import java.net.InetSocketAddress;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Accessors(fluent = true)
@Getter
public class ClientConfig {

  private final Map<ChannelOption<Object>, Object> bootstrapOptions;
  private final String name;
  private final TlsConfig tls;
  private final boolean messageLoggerEnabled;
  private final InetSocketAddress local;
  private final InetSocketAddress remote;
  private final IdleTimeoutConfig idleTimeoutConfig;

  public String getName() {
    return name;
  }

  public TlsConfig getTls() {
    return tls;
  }

  public boolean isMessageLoggerEnabled() {
    return messageLoggerEnabled;
  }

  public ClientConfig(Config config) {
    bootstrapOptions = null;
    name = config.getString("name");
    tls = new TlsConfig(config.getConfig("settings.tls"));
    if (!tls.isUseSsl() && tls.isLogInsecureConfig()) {
      log.warn("Client '{}' has useSsl set to false!", name);
    }
    messageLoggerEnabled = config.getBoolean("settings.messageLoggerEnabled");

    if (config.getString("localIp").isEmpty()) {
      local = null;
    } else {
      local = new InetSocketAddress(config.getString("localIp"), config.getInt("localPort"));
    }
    remote = new InetSocketAddress(config.getString("remoteIp"), config.getInt("remotePort"));

    boolean idleTimeoutEnabled = config.getBoolean("idleTimeoutEnabled");
    int idleTimeoutDuration = 0;
    if (idleTimeoutEnabled) {
      idleTimeoutDuration = config.getInt("idleTimeoutDuration");
    }
    idleTimeoutConfig = new IdleTimeoutConfig(idleTimeoutEnabled, idleTimeoutDuration);
  }

  public boolean isTlsEnabled() {
    return tls.isUseSsl();
  }

  public IdleTimeoutConfig getIdleTimeoutConfig() {
    return idleTimeoutConfig;
  };

  public static ClientConfig fromConfig(String key, Config config) {
    return new ClientConfig(config.getConfig(key));
  }

  public static ClientConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }
}
