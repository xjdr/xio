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

  // TODO(CK): This constructor is goofy and should be replaced with a factory method
  // the new constructor should be ProxyRouteState(ProxyRouteConfig, ProxyHandler,
  // List<ClientStates>)
  public ProxyRouteState(ApplicationState state, ProxyRouteConfig config, ProxyHandler handler) {
    super(ProxyRouteState::buildRoute, config, handler);
    List<ClientConfig> configs = config.clientConfigs();
    clientStates =
        configs
            .stream()
            .map(clientConfig -> new ClientState(clientConfig, state.workerGroup()))
            .collect(Collectors.toList());
  }

  @Override
  public ProxyHandler handler() {
    return (ProxyHandler) super.handler();
  }
}
