package com.xjeffrose.xio.grpc;

import com.google.common.collect.Lists;
import com.xjeffrose.xio.http.RouteConfig;
import com.xjeffrose.xio.http.RouteState;
import io.netty.handler.codec.http.HttpMethod;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class GrpcRouteStateBuilder {
  public List<RouteState> buildGrpcRouteStates(
      List<GrpcService> grpcServices, @Nullable Consumer<GrpcRoute> extraSetupFunction) {
    List<RouteState> routeStates = Lists.newArrayList();

    for (GrpcService service : grpcServices) {
      for (GrpcRoute route : service.getRoutes()) {
        if (extraSetupFunction != null) {
          extraSetupFunction.accept(route);
        }

        List<HttpMethod> methods = Collections.singletonList(HttpMethod.POST);
        String host = "";
        String permissionNeeded = "none";

        RouteConfig config = new RouteConfig(methods, host, route.buildPath(), permissionNeeded);
        routeStates.add(new RouteState(config, route.handler));
      }
    }

    return routeStates;
  }

  public List<RouteState> buildGrpcRouteStates(List<GrpcService> grpcServices) {
    return buildGrpcRouteStates(grpcServices, null);
  }
}
