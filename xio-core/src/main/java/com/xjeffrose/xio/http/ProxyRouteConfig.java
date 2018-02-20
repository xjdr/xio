package com.xjeffrose.xio.http;

import com.typesafe.config.Config;
import com.xjeffrose.xio.client.ClientConfig;
import java.util.List;
import java.util.stream.Collectors;
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
  private final List<ClientConfig> clientConfigs;

  enum ProxyHostPolicy {
    UseRequestHeader,
    UseRemoteAddress,
    UseConfigValue;
  }

  private final ProxyHostPolicy proxyHostPolicy;
  private final String proxyHost; // ideally should match client address:port
  private final String proxyPath; // must end in slash

  private static List<ClientConfig> buildClientConfigs(List<Config> configs) {
    return configs.stream().map(ClientConfig::new).collect(Collectors.toList());
  }

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
}
