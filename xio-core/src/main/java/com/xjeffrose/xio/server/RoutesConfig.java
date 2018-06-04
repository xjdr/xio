package com.xjeffrose.xio.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import com.xjeffrose.xio.http.Route;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * RoutesConfig is the source of all http routing configuration data. This class is meant to store
 * read-only configuration values. Do not put mutable state on this class.
 */
@Accessors(fluent = true)
public class RoutesConfig {

  @Getter
  private Map<com.xjeffrose.xio.http.Route, ProxyRouteConfig> proxyRoutes = new LinkedHashMap<>();

  ImmutableMap<com.xjeffrose.xio.http.Route, ProxyRouteConfig> copyProxyRoutes() {
    return ImmutableMap.copyOf(proxyRoutes);
  }

  @SuppressWarnings("unchecked")
  public RoutesConfig(Config config) {
    addProxyRoutes((List<Config>) config.getConfigList("proxy.routes"));
  }

  private void addProxyRoutes(List<Config> configs) {
    for (Config config : configs) {
      ProxyRouteConfig routeConfig = new ProxyRouteConfig(config);
      com.xjeffrose.xio.http.Route route = Route.build(routeConfig.path() + ":*path");
      proxyRoutes.put(route, routeConfig);
    }
  }

  public ImmutableSet<String> permissionsNeeded() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (ProxyRouteConfig config : proxyRoutes.values()) {
      builder.add(config.permissionNeeded());
    }

    return builder.build();
  }

  public static RoutesConfig fromConfig(String key, Config config) {
    return new RoutesConfig(config.getConfig(key));
  }
}
