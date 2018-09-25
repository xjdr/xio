package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.tls.TlsConfig;
import io.netty.channel.ChannelOption;
import java.net.InetSocketAddress;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

// TODO(CK): rename this to ServerConfig
@Slf4j
public class XioServerConfig {
  @Getter private final Map<ChannelOption<Object>, Object> bootstrapOptions;
  @Getter private final boolean whiteListEnabled;
  @Getter private final boolean blackListEnabled;
  @Getter private String name;
  @Getter private InetSocketAddress bindAddress;
  @Getter private ServerLimits limits;
  @Getter private TlsConfig tls;
  @Getter private final boolean messageLoggerEnabled;

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
    limits = new ServerLimits(config.getConfig("limits"));
    tls = TlsConfig.builderFrom(config.getConfig("settings.tls")).build();
    messageLoggerEnabled = config.getBoolean("settings.messageLoggerEnabled");
    if (!tls.isUseSsl() && tls.isLogInsecureConfig()) {
      log.warn("Server '{}' has useSsl set to false!", name);
    }

    blackListEnabled = config.getBoolean("enableBlackList");
    whiteListEnabled = config.getBoolean("enableWhiteList");
  }

  public static XioServerConfig fromConfig(String key, Config config) {
    return new XioServerConfig(config.getConfig(key));
  }

  public static XioServerConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

  public boolean isTlsEnabled() {
    return tls.isUseSsl();
  }
}
