package com.xjeffrose.xio.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to generate a List<DynamicRouteConfig> based in a JSON string input matching
 * the format described in /test/resources/route_parameters.json
 */
public class DynamicRouteConfigsFactory {

  /**
   * This is a factory method used to invoke the build operation given a JSON input string The
   * output of this file is the raw material used to build ProxyRoutes dynamically from JSON
   */
  public static List<DynamicRouteConfig> build(String string) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    DynamicRouteEntry[] dynamicRouteEntries =
        objectMapper.readValue(string, DynamicRouteEntry[].class);
    return createDynamicRouteConfigs(dynamicRouteEntries);
  }

  private static List<DynamicRouteConfig> createDynamicRouteConfigs(
      DynamicRouteEntry[] dynamicRouteEntries) {
    ArrayList<DynamicRouteConfig> dynamicRouteconfigs = new ArrayList<>();
    for (DynamicRouteEntry dynamicRouteEntry : dynamicRouteEntries) {
      List<DynamicClientConfig> dynamicClientConfigs =
          createDynamicClientConfigs(dynamicRouteEntry);
      dynamicRouteconfigs.add(
          new DynamicRouteConfig(
              dynamicRouteEntry.getName(), dynamicRouteEntry.getPath(), dynamicClientConfigs));
    }
    return dynamicRouteconfigs;
  }

  private static List<DynamicClientConfig> createDynamicClientConfigs(
      DynamicRouteEntry dynamicRouteEntry) {
    ArrayList<DynamicClientConfig> dynamicClientConfigs = new ArrayList<>();
    for (String clientIp : dynamicRouteEntry.getClientsIps()) {
      dynamicClientConfigs.add(
          new DynamicClientConfig(
              dynamicRouteEntry.getClientName(),
              clientIp,
              dynamicRouteEntry.getPort(),
              dynamicRouteEntry.isTlsEnabled()));
    }
    return dynamicClientConfigs;
  }
}
