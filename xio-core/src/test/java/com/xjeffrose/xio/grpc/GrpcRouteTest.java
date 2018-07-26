package com.xjeffrose.xio.grpc;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class GrpcRouteTest {
  GrpcService service = null;

  @Before
  public void beforeEach() {
    service =
        new GrpcService() {
          @Override
          public List<GrpcRoute> getRoutes() {
            return null;
          }

          @Override
          public String getPackageName() {
            return "package_name";
          }

          @Override
          public String getServiceName() {
            return "service_name";
          }
        };
  }

  @Test
  public void testBuildPath() {
    String methodName = "MethodName";
    GrpcRoute subject = new GrpcRoute(service, methodName, null);

    String expectedPath =
        "/" + service.getPackageName() + "." + service.getServiceName() + "/" + methodName;
    assertEquals(expectedPath, subject.buildPath());
  }

  @Test
  public void testBuildOu() {
    GrpcRoute subject = new GrpcRoute(service, null, null);

    String expectedOu = service.getPackageName() + "." + service.getServiceName();
    assertEquals(expectedOu, subject.buildOu());
  }
}
