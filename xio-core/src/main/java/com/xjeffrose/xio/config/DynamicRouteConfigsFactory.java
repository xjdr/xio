package com.xjeffrose.xio.config;

import com.google.gson.Gson;
import java.util.ArrayList;

public class DynamicRouteConfigsFactory {

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
