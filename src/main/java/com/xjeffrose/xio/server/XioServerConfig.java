package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.netty.channel.ChannelOption;
import io.netty.util.Timer;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.net.InetSocketAddress;

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
    bindAddress = new InetSocketAddress(config.getString("settings.bindHost"), config.getInt("settings.bindPort"));
    limits = new XioServerLimits(config.getConfig("limits"));
    tls = new TlsConfig(config.getConfig("settings.tls"));
    messageLoggerEnabled = config.getBoolean("settings.messageLoggerEnabled");
  }

  static public XioServerConfig fromConfig(String key, Config config) {
    return new XioServerConfig(config.getConfig(key));
  }

  static public XioServerConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

}
