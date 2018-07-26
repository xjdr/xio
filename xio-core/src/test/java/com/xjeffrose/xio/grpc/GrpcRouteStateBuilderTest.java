package com.xjeffrose.xio.grpc;

import com.xjeffrose.xio.http.GrpcRequestHandler;
import com.xjeffrose.xio.http.RouteState;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GrpcRouteStateBuilderTest extends Assert {
  private GrpcRouteStateBuilder subject = null;

  @Before
  public void beforeEach() {
    subject = new GrpcRouteStateBuilder();
  }

  @Test
  public void testGrpcRoute() {
    TestGrpcService grpcService = new TestGrpcService();
    GrpcRoute grpcRoute = grpcService.getRoutes().get(0);

    List<RouteState> routeStates = subject.buildGrpcRouteStates(Collections.singletonList(grpcService));

    RouteState registerServiceRouteState = routeStates.get(0);
    assertNotNull(registerServiceRouteState);

    assertEquals(Collections.singletonList(HttpMethod.POST), registerServiceRouteState.config().methods());
    assertEquals("", registerServiceRouteState.config().host());
    assertEquals(grpcRoute.buildPath(), registerServiceRouteState.path());
    assertEquals("*", registerServiceRouteState.config().permissionNeeded());
    assertEquals(grpcRoute.handler, registerServiceRouteState.handler());
  }

  @Test
  public void testBuildingAllRoutes() {
    TestGrpcService grpcService1 = new TestGrpcService();
    grpcService1.pagackName = "package_name_1";
    grpcService1.serviceName = "service_name_1";
    grpcService1.routes = Arrays.asList(
      new GrpcRoute(grpcService1, "method_name_1_A", new GrpcRequestHandler<>(null, null)),
      new GrpcRoute(grpcService1, "method_name_1_B", new GrpcRequestHandler<>(null, null))
    );

    TestGrpcService grpcService2 = new TestGrpcService();
    grpcService2.pagackName = "package_name_2";
    grpcService2.serviceName = "service_name_2";
    grpcService2.routes = Arrays.asList(
      new GrpcRoute(grpcService1, "method_name_2_A", new GrpcRequestHandler<>(null, null)),
      new GrpcRoute(grpcService1, "method_name_2_B", new GrpcRequestHandler<>(null, null)),
      new GrpcRoute(grpcService1, "method_name_2_C", new GrpcRequestHandler<>(null, null))
    );

    List<RouteState> routeStates = subject.buildGrpcRouteStates(Arrays.asList(grpcService1, grpcService2));

    // 2 routes from service 1 plus 3 routes from service 2 equals 5 expected routes
    assertEquals(5, routeStates.size());
  }

  private class TestGrpcService implements GrpcService {
    String pagackName = "package_name";
    String serviceName = "service_name";
    List<GrpcRoute> routes = Collections.singletonList(new GrpcRoute(this, "method_name", new GrpcRequestHandler<>(null, null)));

    @Override
    public String getPackageName() {
      return pagackName;
    }

    @Override
    public String getServiceName() {
      return serviceName;
    }

    @Override
    public List<GrpcRoute> getRoutes() {
      return routes;
    }
  }
}
