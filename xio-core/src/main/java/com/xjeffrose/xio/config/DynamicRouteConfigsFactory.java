package com.xjeffrose.xio.config;

import com.google.gson.Gson;
import java.util.ArrayList;

/**
 * This class is used to generate a List<DynamicRouteConfig> based in a JSON string input matching the format
 * described in /test/resources/route_parameters.json
 */
public class DynamicRouteConfigsFactory {

  /**
   * This is a factory method used to invoke the build operation given a JSON input string
   * The output of this file is the raw material used to build ProxyRoutes dynamically from JSON
   */
  public static ArrayList<DynamicRouteConfig> build(String string) {
    Gson gson = new Gson();
    DynamicRouteEntry[] dynamicRouteEntries = gson.fromJson(string, DynamicRouteEntry[].class);
    return createDynamicRouteConfigs(dynamicRouteEntries);
  }

  private static ArrayList<DynamicRouteConfig> createDynamicRouteConfigs(
      DynamicRouteEntry[] dynamicRouteEntries) {
    ArrayList<DynamicRouteConfig> dynamicRouteconfigs = new ArrayList<>();
    for (DynamicRouteEntry dynamicRouteEntry : dynamicRouteEntries) {
      ArrayList<DynamicClientConfig> dynamicClientConfigs =
          createDynamicClientConfigs(dynamicRouteEntry);
      dynamicRouteconfigs.add(
          new DynamicRouteConfig(
              dynamicRouteEntry.getName(), dynamicRouteEntry.getPath(), dynamicClientConfigs));
    }
    return dynamicRouteconfigs;
  }

  private static ArrayList<DynamicClientConfig> createDynamicClientConfigs(
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
