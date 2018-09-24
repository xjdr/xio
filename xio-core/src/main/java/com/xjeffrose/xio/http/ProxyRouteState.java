package com.xjeffrose.xio.http;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.client.ClientConfig;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class ProxyRouteState extends RouteState {
  private final List<ClientState> clientStates;

  public static Route buildRoute(RouteConfig config) {
    return Route.build(config.path() + ":*path");
  }

  public static ProxyRouteState create(
      ApplicationState state, ProxyRouteConfig config, ProxyHandler handler) {
    List<ClientConfig> configs = config.clientConfigs();
    List<ClientState> clientStates =
        configs
            .stream()
            .map(clientConfig -> new ClientState(clientConfig, state.workerGroup()))
            .collect(Collectors.toList());
    return new ProxyRouteState(config, handler, clientStates);
  }

  // copy constructor with new clientstates
  public ProxyRouteState(ProxyRouteState routeState, List<ClientState> clientStates) {
    super(routeState);
    this.clientStates = clientStates;
  }

  public ProxyRouteState(
      ProxyRouteConfig config, ProxyHandler handler, List<ClientState> clientStates) {
    super(ProxyRouteState::buildRoute, config, handler);
    this.clientStates = clientStates;
  }

  @Override
  public ProxyHandler handler() {
    return (ProxyHandler) super.handler();
  }
}
