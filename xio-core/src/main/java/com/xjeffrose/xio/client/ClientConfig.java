package com.xjeffrose.xio.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.tls.TlsConfig;
import io.netty.channel.ChannelOption;
import java.net.InetSocketAddress;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Accessors(fluent = true)
@Getter
public class ClientConfig {

  private final Map<ChannelOption<Object>, Object> bootstrapOptions;
  private final TlsConfig tls;
  private final boolean messageLoggerEnabled;
  private final InetSocketAddress local;
  private final InetSocketAddress remote;
  private final IdleTimeoutConfig idleTimeoutConfig;

  public TlsConfig getTls() {
    return tls;
  }

  public boolean isMessageLoggerEnabled() {
    return messageLoggerEnabled;
  }

  public static ClientConfig from(Config config) {
    TlsConfig tls = TlsConfig.builderFrom(config.getConfig("settings.tls")).build();
    boolean messageLoggerEnabled = config.getBoolean("settings.messageLoggerEnabled");

    InetSocketAddress local = null;
    if (!config.getString("localIp").isEmpty()) {
      local = new InetSocketAddress(config.getString("localIp"), config.getInt("localPort"));
    }

    InetSocketAddress remote =
        new InetSocketAddress(config.getString("remoteIp"), config.getInt("remotePort"));

    boolean idleTimeoutEnabled = config.getBoolean("idleTimeoutEnabled");
    int idleTimeoutDuration = 0;
    if (idleTimeoutEnabled) {
      idleTimeoutDuration = config.getInt("idleTimeoutDuration");
    }
    IdleTimeoutConfig idleTimeoutConfig =
        new IdleTimeoutConfig(idleTimeoutEnabled, idleTimeoutDuration);

    return new ClientConfig(null, tls, messageLoggerEnabled, local, remote, idleTimeoutConfig);
  }

  public ClientConfig(
      Map<ChannelOption<Object>, Object> bootstrapOptions,
      TlsConfig tls,
      boolean messageLoggerEnabled,
      InetSocketAddress local,
      InetSocketAddress remote,
      IdleTimeoutConfig idleTimeoutConfig) {
    this.bootstrapOptions = bootstrapOptions;
    this.tls = tls;
    if (!tls.isUseSsl() && tls.isLogInsecureConfig()) {
      log.warn("Client '{}' has useSsl set to false!", remote.toString());
    }
    this.messageLoggerEnabled = messageLoggerEnabled;
    this.local = local;
    this.remote = remote;
    this.idleTimeoutConfig = idleTimeoutConfig;
  }

  public boolean isTlsEnabled() {
    return tls.isUseSsl();
  }

  public IdleTimeoutConfig getIdleTimeoutConfig() {
    return idleTimeoutConfig;
  };

  public static ClientConfig fromConfig(String key, Config config) {
    return ClientConfig.from(config.getConfig(key));
  }

  public static ClientConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

  public static ClientConfig.Builder newBuilder(ClientConfig fallbackObject) {
    return new ClientConfig.Builder(fallbackObject);
  }

  /**
   * Used to create a ClientConfig at runtime.
   *
   * <p>If a value is not set, it defaults to using the fallbackObject's's value.
   */
  public static class Builder {
    private ClientConfig fallbackObject;
    private Map<ChannelOption<Object>, Object> bootstrapOptions;
    private TlsConfig tls;
    private boolean messageLoggerEnabled;
    private InetSocketAddress local;
    private InetSocketAddress remote;
    private IdleTimeoutConfig idleTimeoutConfig;

    private Builder(ClientConfig fallbackObject) {
      this.fallbackObject = fallbackObject;
    }

    public Builder setBootstrapOptions(Map<ChannelOption<Object>, Object> bootstrapOptions) {
      this.bootstrapOptions = bootstrapOptions;
      return this;
    }

    public Builder setTls(TlsConfig tls) {
      this.tls = tls;
      return this;
    }

    public Builder setMessageLoggerEnabled(boolean messageLoggerEnabled) {
      this.messageLoggerEnabled = messageLoggerEnabled;
      return this;
    }

    public Builder setLocal(InetSocketAddress local) {
      this.local = local;
      return this;
    }

    public Builder setRemote(InetSocketAddress remote) {
      this.remote = remote;
      return this;
    }

    public Builder setIdleTimeoutConfig(IdleTimeoutConfig idleTimeoutConfig) {
      this.idleTimeoutConfig = idleTimeoutConfig;
      return this;
    }

    public ClientConfig build() {
      return new ClientConfig(
          valueOrFallback(bootstrapOptions, fallbackObject.bootstrapOptions()),
          valueOrFallback(tls, fallbackObject.tls()),
          valueOrFallback(messageLoggerEnabled, fallbackObject.messageLoggerEnabled()),
          valueOrFallback(local, fallbackObject.local()),
          valueOrFallback(remote, fallbackObject.remote()),
          valueOrFallback(idleTimeoutConfig, fallbackObject.idleTimeoutConfig()));
    }

    private <T> T valueOrFallback(@Nullable T value, T fallback) {
      return value != null ? value : fallback;
    }
  }
}
