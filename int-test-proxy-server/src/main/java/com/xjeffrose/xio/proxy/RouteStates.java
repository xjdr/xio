package com.xjeffrose.xio.proxy;

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.http.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RouteStates {
  private final AtomicReference<ImmutableMap<String, RouteState>> routeMap;

  public RouteStates(
      ProxyRouteConfig proxyRouteConfig,
      ApplicationState applicationState,
      ProxyClientFactory clientFactory) {
    List<ProxyRouteState> proxyRouteStates =
        Stream.of(proxyRouteConfig)
            .map(
                (ProxyRouteConfig config) ->
                    ProxyRouteState.create(
                        applicationState,
                        config,
                        new PersistentProxyHandler(
                            clientFactory, config, new SocketAddressHelper())))
            .collect(Collectors.toList());

    LinkedHashMap<String, ProxyRouteState> routeMap = new LinkedHashMap<>();
    proxyRouteStates.forEach((ProxyRouteState state) -> routeMap.put(state.path(), state));

    this.routeMap = new AtomicReference<>(ImmutableMap.copyOf(routeMap));
  }

  public ImmutableMap<String, RouteState> routeMap() {
    return routeMap.get();
  }
}
