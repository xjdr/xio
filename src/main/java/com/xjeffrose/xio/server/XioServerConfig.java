package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.net.InetSocketAddress;

@Slf4j
public class XioServerConfig {
  @Getter
  private final Map<ChannelOption<Object>, Object> bootstrapOptions;
  @Getter
  private String name;
  @Getter
  private InetSocketAddress bindAddress;
  @Getter
  private XioServerLimits limits;
  @Getter
  private TlsConfig tls;
  @Getter
  private final boolean messageLoggerEnabled;

  public XioServerConfig(Config config) {
    bootstrapOptions = null;
    name = config.getString("name");
    String address;
    if (config.hasPath("settings.bindHost")) {
      address = config.getString("settings.bindHost");
      log.warn("settings.bindHost is deprecated please use settings.bindIp");
    } else {
      address = config.getString("settings.bindIp");
    }

    bindAddress = new InetSocketAddress(address, config.getInt("settings.bindPort"));
    limits = new XioServerLimits(config.getConfig("limits"));
    tls = new TlsConfig(config.getConfig("settings.tls"));
    messageLoggerEnabled = config.getBoolean("settings.messageLoggerEnabled");
    if (!tls.isUseSsl() && tls.isLogInsecureConfig()) {
      log.warn("Server '{}' has useSsl set to false!", name);
    }
  }

  static public XioServerConfig fromConfig(String key, Config config) {
    return new XioServerConfig(config.getConfig(key));
  }

  static public XioServerConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

}
