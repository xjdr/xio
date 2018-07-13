package com.xjeffrose.xio.http;

import com.typesafe.config.Config;
import com.xjeffrose.xio.client.ClientConfig;
import io.netty.handler.codec.http.HttpMethod;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

/*
url = scheme, authority, path, query-string, fragment
{
  methods: [] // any
  host: "" // any
  path: "/foo/bar/"
  permissionNeeded: "" // any
  client {
    address: "10.10.10.10:5678"
  }
  clients = [
    {
      address: "10.10.10.10:5678"
    }
    {
      address: "11.11.11.11:5678"
    }
  ]
  proxyHostPolicy: "UseConfigValue"
  proxyHost: "google.com:5678"
  proxyPath: "/rabid/rabbit/"
}
 */

@Accessors(fluent = true)
@Getter
public class ProxyRouteConfig extends RouteConfig {
  enum ProxyHostPolicy {
    UseRequestHeader,
    UseRemoteAddress,
    UseConfigValue;
  }

  private final List<ClientConfig> clientConfigs;
  private final ProxyHostPolicy proxyHostPolicy;
  private final String proxyHost; // ideally should match client address:port
  private final String proxyPath; // must end in slash

  private static List<ClientConfig> buildClientConfigs(List<Config> configs) {
    return configs.stream().map(ClientConfig::from).collect(Collectors.toList());
  }

  // TODO(br): find a way to combine preconditions
  public ProxyRouteConfig(Config config) {
    super(config);
    // validatePath(config.getString("path"), config.origin());
    endsWith(config, "path", "/");
    //    List<Config> foo = config.getConfigList("clients");
    // clientConfigs = buildClientConfigs(foo);
    clientConfigs = buildClientConfigs((List<Config>) config.getConfigList("clients"));
    // clientConfigs = buildClientConfigs(new ArrayList<>());
    proxyHostPolicy = config.getEnum(ProxyHostPolicy.class, "proxyHostPolicy");
    proxyHost = config.getString("proxyHost");
    proxyPath = startsWith(config, "proxyPath", "/");
    endsWith(config, "proxyPath", "/");
    // validatePath(config.getString("proxyPath"), config.origin());
  }

  public ProxyRouteConfig(
      List<HttpMethod> methods,
      String host,
      String path,
      String permissionNeeded,
      List<ClientConfig> clientConfigs,
      ProxyHostPolicy proxyHostPolicy,
      String proxyHost,
      String proxyPath) {
    super(methods, host, path, permissionNeeded);
    ensureEndsWith("path", path, "/");
    ensureStartsWith("proxyPath", proxyPath, "/");
    ensureEndsWith("proxyPath", proxyPath, "/");

    this.clientConfigs = clientConfigs;
    this.proxyHostPolicy = proxyHostPolicy;
    this.proxyHost = proxyHost;
    this.proxyPath = proxyPath;
  }

  public static Builder newBuilder(ProxyRouteConfig fallbackObject) {
    return new Builder(fallbackObject);
  }

  /**
   * Used to create a ProxyRouteConfig at runtime.
   *
   * <p>If a value is not set, it defaults to using the default object's value.
   */
  public static class Builder {
    private ProxyRouteConfig fallbackObject;
    private List<HttpMethod> methods;
    private String host;
    private String path;
    private String permissionNeeded;
    private List<ClientConfig> clientConfigs;
    private ProxyHostPolicy proxyHostPolicy;
    private String proxyHost;
    private String proxyPath;

    private Builder(ProxyRouteConfig fallbackObject) {
      this.fallbackObject = fallbackObject;
    }

    public Builder setMethods(List<HttpMethod> methods) {
      this.methods = methods;
      return this;
    }

    public Builder setHost(String host) {
      this.host = host;
      return this;
    }

    public Builder setPath(String path) {
      this.path = path;
      return this;
    }

    public Builder setPermissionNeeded(String permissionNeeded) {
      this.permissionNeeded = permissionNeeded;
      return this;
    }

    public Builder setClientConfigs(List<ClientConfig> clientConfigs) {
      this.clientConfigs = clientConfigs;
      return this;
    }

    public Builder setProxyHostPolicy(ProxyHostPolicy proxyHostPolicy) {
      this.proxyHostPolicy = proxyHostPolicy;
      return this;
    }

    public Builder setProxyHost(String proxyHost) {
      this.proxyHost = proxyHost;
      return this;
    }

    public Builder setProxyPath(String proxyPath) {
      this.proxyPath = proxyPath;
      return this;
    }

    public ProxyRouteConfig build() {
      return new ProxyRouteConfig(
          valueOrFallback(methods, fallbackObject.methods()),
          valueOrFallback(host, fallbackObject.host()),
          valueOrFallback(path, fallbackObject.path()),
          valueOrFallback(permissionNeeded, fallbackObject.permissionNeeded()),
          valueOrFallback(clientConfigs, fallbackObject.clientConfigs()),
          valueOrFallback(proxyHostPolicy, fallbackObject.proxyHostPolicy()),
          valueOrFallback(proxyHost, fallbackObject.proxyHost()),
          valueOrFallback(proxyPath, fallbackObject.proxyPath()));
    }

    private <T> T valueOrFallback(@Nullable T value, T fallback) {
      return value != null ? value : fallback;
    }
  }
}
