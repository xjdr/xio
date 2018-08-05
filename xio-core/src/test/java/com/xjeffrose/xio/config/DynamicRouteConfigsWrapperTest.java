package com.xjeffrose.xio.config;

import com.google.gson.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletRegistration;
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
    byte[] encoded = Files.readAllBytes(Paths.get("route_parameters.json"));
    String testString = new String(encoded, Charset.defaultCharset());
    subject = new DynamicRouteConfigsWrapper(testString);
  }

  @Test
  public void testGenerationOfDynamicRouteConfigs_valid_config() throws Exception {
    ArrayList<DynamicRouteConfig> results = subject.getDynamicRouteConfigs();
    assertEquals(3, results.size());

    DynamicRouteConfig resultRouteconfig1 = subject.getDynamicRouteConfigs().get(0);
    assertEquals(resultRouteconfig1.getName(), "route1");
    assertEquals(resultRouteconfig1.getPath(), "/path1/");
    assertEquals(resultRouteconfig1.getClientConfigs().size(), 2);
    assertEquals(resultRouteconfig1.getClientConfigs().get(0).getClientName(), "client1");
    assertEquals(resultRouteconfig1.getClientConfigs().get(0).getIpAddress(), "1.2.3.4");
    assertEquals(resultRouteconfig1.getClientConfigs().get(0).getPort(), 1234);
    assertEquals(resultRouteconfig1.getClientConfigs().get(0).isTlsEnabled(), false);
    assertEquals(resultRouteconfig1.getClientConfigs().get(1).getClientName(), "client1");
    assertEquals(resultRouteconfig1.getClientConfigs().get(1).getIpAddress(), "1.2.3.5");
    assertEquals(resultRouteconfig1.getClientConfigs().get(1).getPort(), 1234);
    assertEquals(resultRouteconfig1.getClientConfigs().get(1).isTlsEnabled(), false);

    DynamicRouteConfig resultRouteconfig2 = subject.getDynamicRouteConfigs().get(1);
    assertEquals(resultRouteconfig2.getName(), "route2");
    assertEquals(resultRouteconfig2.getPath(), "/path2/");
    assertEquals(resultRouteconfig2.getClientConfigs().size(), 2);
    assertEquals(resultRouteconfig2.getClientConfigs().get(0).getClientName(), "client2");
    assertEquals(resultRouteconfig2.getClientConfigs().get(0).getIpAddress(), "2.2.3.4");
    assertEquals(resultRouteconfig2.getClientConfigs().get(0).getPort(), 5678);
    assertEquals(resultRouteconfig2.getClientConfigs().get(0).isTlsEnabled(), true);
    assertEquals(resultRouteconfig2.getClientConfigs().get(1).getClientName(), "client2");
    assertEquals(resultRouteconfig2.getClientConfigs().get(1).getIpAddress(), "2.2.3.5");
    assertEquals(resultRouteconfig2.getClientConfigs().get(1).getPort(), 5678);
    assertEquals(resultRouteconfig2.getClientConfigs().get(1).isTlsEnabled(), true);

    DynamicRouteConfig resultRouteconfig3 = subject.getDynamicRouteConfigs().get(2);
    assertEquals(resultRouteconfig2.getName(), "route3");
    assertEquals(resultRouteconfig2.getPath(), "/path3/");
    assertEquals(resultRouteconfig2.getClientConfigs().size(), 0);
  }
}
