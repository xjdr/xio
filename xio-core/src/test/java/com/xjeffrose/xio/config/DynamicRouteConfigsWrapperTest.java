package com.xjeffrose.xio.config;

import com.google.gson.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletRegistration;
import java.io.File;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DynamicRouteConfigsWrapperTest extends Assert {

  DynamicRouteConfigsWrapper subject;
  JsonArray expectedResults;

  @Before
  public void before() throws Exception {
    ClassLoader classLoader = new DynamicRouteConfigsWrapperTest().getClass().getClassLoader();
    File file = new File(classLoader.getResource("route_parameters.json").getFile());
    String content = new String(Files.readAllBytes(file.toPath()));
    subject = new DynamicRouteConfigsWrapper(content);
  }

  @Test
  public void testGenerationOfDynamicRouteConfigs_valid_config() throws Exception {
    ArrayList<DynamicRouteConfig> results = subject.getDynamicRouteConfigs();
    assertEquals(3, results.size());

    ArrayList<DynamicClientConfig> clientConfigs1 = new ArrayList<>();
    clientConfigs1.add(new DynamicClientConfig("client1", "1.2.3.4", 1234, false));
    clientConfigs1.add(new DynamicClientConfig("client1", "1.2.3.5", 1234, false));
    DynamicRouteConfig expectedRouteConfig1 = new DynamicRouteConfig("route1", "/path1/", clientConfigs1);

    ArrayList<DynamicClientConfig> clientConfigs2 = new ArrayList<>();
    clientConfigs2.add(new DynamicClientConfig("client2", "2.2.3.4", 5678, true));
    clientConfigs2.add(new DynamicClientConfig("client2", "2.2.3.5", 5678, true));
    DynamicRouteConfig expectedRouteConfig2 = new DynamicRouteConfig("route2", "/path2/", clientConfigs2);

    ArrayList<DynamicClientConfig> clientConfigs3 = new ArrayList<>();
    DynamicRouteConfig expectedRouteConfig3 = new DynamicRouteConfig("route3", "/path3/", clientConfigs3);

    DynamicRouteConfig resultRouteconfig1 = subject.getDynamicRouteConfigs().get(0);
    DynamicRouteConfig resultRouteconfig2 = subject.getDynamicRouteConfigs().get(1);
    DynamicRouteConfig resultRouteconfig3 = subject.getDynamicRouteConfigs().get(2);

    assertEquals(expectedRouteConfig1, resultRouteconfig1);
    assertEquals(expectedRouteConfig2, resultRouteconfig2);
    assertEquals(expectedRouteConfig3, resultRouteconfig3);
  }

  @Test
  public void testGenerationOfDynamicRouteConfigs_valid_config_mismatch() throws Exception {
    ArrayList<DynamicRouteConfig> results = subject.getDynamicRouteConfigs();
    assertEquals(3, results.size());

    ArrayList<DynamicClientConfig> clientConfigs1 = new ArrayList<>();
    clientConfigs1.add(new DynamicClientConfig("client1bad", "1.2.3.4bad", 12340, false));
    clientConfigs1.add(new DynamicClientConfig("client1bad", "1.2.3.5bad", 12340, false));
    DynamicRouteConfig expectedRouteConfig1 = new DynamicRouteConfig("route1", "/path1/", clientConfigs1);

    ArrayList<DynamicClientConfig> clientConfigs2 = new ArrayList<>();
    clientConfigs2.add(new DynamicClientConfig("client2bad", "2.2.3.4bad", 56780, true));
    clientConfigs2.add(new DynamicClientConfig("client2bad", "2.2.3.5bad", 56780, true));
    DynamicRouteConfig expectedRouteConfig2 = new DynamicRouteConfig("route2bad", "/path2/bad", clientConfigs2);

    ArrayList<DynamicClientConfig> clientConfigs3 = new ArrayList<>();
    DynamicRouteConfig expectedRouteConfig3 = new DynamicRouteConfig("route3bad", "/path3/bad", clientConfigs3);

    DynamicRouteConfig resultRouteconfig1 = subject.getDynamicRouteConfigs().get(0);
    DynamicRouteConfig resultRouteconfig2 = subject.getDynamicRouteConfigs().get(1);
    DynamicRouteConfig resultRouteconfig3 = subject.getDynamicRouteConfigs().get(2);

    assertNotEquals(expectedRouteConfig1, resultRouteconfig1);
    assertNotEquals(expectedRouteConfig2, resultRouteconfig2);
    assertNotEquals(expectedRouteConfig3, resultRouteconfig3);
  }
}
