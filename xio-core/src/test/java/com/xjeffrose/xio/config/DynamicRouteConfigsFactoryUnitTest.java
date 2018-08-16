package com.xjeffrose.xio.config;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.xjeffrose.xio.http.RouteConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DynamicRouteConfigsFactoryUnitTest extends Assert {

  public String buildContent(String filename) throws Exception {
    ClassLoader classLoader = new DynamicRouteConfigsFactoryUnitTest().getClass().getClassLoader();
    File file = new File(classLoader.getResource(filename).getFile());
    return new String(Files.readAllBytes(file.toPath()));
  }

  @Test
  public void testGenerationOfDynamicRouteConfigs_valid_config() throws Exception {
    String content = buildContent("route_parameters.json");
    List<DynamicRouteConfig> results = DynamicRouteConfigsFactory.build(content);
    assertEquals(3, results.size());

    List<DynamicClientConfig> clientConfigs1 = new ArrayList<>();
    clientConfigs1.add(new DynamicClientConfig("client1", "1.2.3.4", 1234, false));
    clientConfigs1.add(new DynamicClientConfig("client1", "1.2.3.5", 1234, false));
    DynamicRouteConfig expectedRouteConfig1 =
        new DynamicRouteConfig("route1", "/path1/", clientConfigs1);

    List<DynamicClientConfig> clientConfigs2 = new ArrayList<>();
    clientConfigs2.add(new DynamicClientConfig("client2", "2.2.3.4", 5678, true));
    clientConfigs2.add(new DynamicClientConfig("client2", "2.2.3.5", 5678, true));
    DynamicRouteConfig expectedRouteConfig2 =
        new DynamicRouteConfig("route2", "/path2/", clientConfigs2);

    List<DynamicClientConfig> clientConfigs3 = new ArrayList<>();
    DynamicRouteConfig expectedRouteConfig3 =
        new DynamicRouteConfig("route3", "/path3/", clientConfigs3);

    // lets sort the results =p
    DynamicRouteConfig resultRouteconfig1 = results.get(0);
    DynamicRouteConfig resultRouteconfig2 = results.get(1);
    DynamicRouteConfig resultRouteconfig3 = results.get(2);

    assertEquals(expectedRouteConfig1, resultRouteconfig1);
    assertEquals(expectedRouteConfig2, resultRouteconfig2);
    assertEquals(expectedRouteConfig3, resultRouteconfig3);
  }

  @Test
  public void testGenerationOfDynamicRouteConfigs_valid_config_with_multiple_routes_of_the_same_path() throws Exception {
    String content = buildContent("route_parameters_with_multiple_endpoints_for_a_path.json");
    List<DynamicRouteConfig> results = DynamicRouteConfigsFactory.build(content);
    assertEquals(3, results.size());

    List<DynamicClientConfig> clientConfigs1 = new ArrayList<>();
    clientConfigs1.add(new DynamicClientConfig("client1", "1.2.3.4", 1234, false));
    clientConfigs1.add(new DynamicClientConfig("client1", "1.2.3.5", 1234, false));
    clientConfigs1.add(new DynamicClientConfig("client1-new", "1.2.3.6", 1235, true));
    clientConfigs1.add(new DynamicClientConfig("client1-new", "1.2.3.7", 1235, true));
    DynamicRouteConfig expectedRouteConfig1 =
      new DynamicRouteConfig("route1", "/path1/", clientConfigs1);

    List<DynamicClientConfig> clientConfigs2 = new ArrayList<>();
    clientConfigs2.add(new DynamicClientConfig("client2", "2.2.3.4", 5678, true));
    clientConfigs2.add(new DynamicClientConfig("client2", "2.2.3.5", 5678, true));
    DynamicRouteConfig expectedRouteConfig2 =
      new DynamicRouteConfig("route2", "/path2/", clientConfigs2);

    List<DynamicClientConfig> clientConfigs3 = new ArrayList<>();
    DynamicRouteConfig expectedRouteConfig3 =
      new DynamicRouteConfig("route3", "/path3/", clientConfigs3);

    // lets sort the results =p
    DynamicRouteConfig resultRouteconfig1 = results.get(0);
    DynamicRouteConfig resultRouteconfig2 = results.get(1);
    DynamicRouteConfig resultRouteconfig3 = results.get(2);

    assertEquals(expectedRouteConfig1, resultRouteconfig1);
    assertEquals(expectedRouteConfig2, resultRouteconfig2);
    assertEquals(expectedRouteConfig3, resultRouteconfig3);
  }

  @Test
  public void testGenerationOfDynamicRouteConfigs_valid_config_mismatch() throws Exception {
    String content = buildContent("route_parameters.json");
    List<DynamicRouteConfig> results = DynamicRouteConfigsFactory.build(content);
    assertEquals(3, results.size());

    List<DynamicClientConfig> clientConfigs1 = new ArrayList<>();
    clientConfigs1.add(new DynamicClientConfig("client1bad", "1.2.3.4bad", 12340, false));
    clientConfigs1.add(new DynamicClientConfig("client1bad", "1.2.3.5bad", 12340, false));
    DynamicRouteConfig expectedRouteConfig1 =
        new DynamicRouteConfig("route1", "/path1/", clientConfigs1);

    List<DynamicClientConfig> clientConfigs2 = new ArrayList<>();
    clientConfigs2.add(new DynamicClientConfig("client2bad", "2.2.3.4bad", 56780, true));
    clientConfigs2.add(new DynamicClientConfig("client2bad", "2.2.3.5bad", 56780, true));
    DynamicRouteConfig expectedRouteConfig2 =
        new DynamicRouteConfig("route2bad", "/path2/bad", clientConfigs2);

    List<DynamicClientConfig> clientConfigs3 = new ArrayList<>();
    DynamicRouteConfig expectedRouteConfig3 =
        new DynamicRouteConfig("route3bad", "/path3/bad", clientConfigs3);

    DynamicRouteConfig resultRouteconfig1 = results.get(0);
    DynamicRouteConfig resultRouteconfig2 = results.get(1);
    DynamicRouteConfig resultRouteconfig3 = results.get(2);

    assertNotEquals(expectedRouteConfig1, resultRouteconfig1);
    assertNotEquals(expectedRouteConfig2, resultRouteconfig2);
    assertNotEquals(expectedRouteConfig3, resultRouteconfig3);
  }
}
