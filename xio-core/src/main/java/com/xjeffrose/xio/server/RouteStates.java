package com.xjeffrose.xio.server;

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.http.*;
import com.xjeffrose.xio.http.Route;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RouteStates {
  private final AtomicReference<ImmutableMap<String, ? extends RouteState>> routeMap;

  public RouteStates(
      ApplicationConfig applicationConfig,
      ApplicationState nfeState,
      ProxyClientFactory clientFactory) {
    LinkedHashMap<String, ProxyRouteState> routeMap = new LinkedHashMap<>();
    applicationConfig
        .proxyRoutes()
        .entrySet()
        // iterate over a stream of ProxyRouteConfig
        .stream()
        // for each ProxyRouteConfig create a ProxyRouteState
        .map(
            (Map.Entry<Route, ProxyRouteConfig> entry) -> {
              ProxyRouteConfig config = entry.getValue();
              return new ProxyRouteState(
                  nfeState,
                  config,
                  new PersistentProxyHandler(clientFactory, config, new SocketAddressHelper()));
            })
        // collect into a List<ProxyRouteState>
        .collect(Collectors.toList())
        .forEach(
            (ProxyRouteState state) -> {
              // put this state into the routeMap with the path as the key
              routeMap.put(state.path(), state);
            });

    this.routeMap = new AtomicReference<>(ImmutableMap.copyOf(routeMap));
  }

  public ImmutableMap<String, RouteState> routeMap() {
    return (ImmutableMap<String, RouteState>) routeMap.get();
  }
}
