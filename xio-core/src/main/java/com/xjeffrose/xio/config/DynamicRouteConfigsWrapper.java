package com.xjeffrose.xio.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;

public class DynamicRouteConfigsWrapper {

  private final ArrayList<DynamicRouteConfig> dynamicRouteConfigs;

  public DynamicRouteConfigsWrapper(String string) {
      Gson gson = new Gson();
      DynamicRouteEntry[] dynamicRouteEntries = gson.fromJson(string, DynamicRouteEntry[].class);
      dynamicRouteConfigs = createDynamicRouteConfigs(dynamicRouteEntries);
  }

  public ArrayList<DynamicRouteConfig> getDynamicRouteConfigs() {
    return dynamicRouteConfigs;
  }

  private ArrayList<DynamicRouteConfig> createDynamicRouteConfigs(DynamicRouteEntry[] dynamicRouteEntries) {
    ArrayList<DynamicRouteConfig> dynamicRouteconfigs = new ArrayList<>();
    for (DynamicRouteEntry dynamicRouteEntry : dynamicRouteEntries) {
      ArrayList<DynamicClientConfig> dynamicClientConfigs = createDynamicClientConfigs(dynamicRouteEntry);
      dynamicRouteconfigs.add(new DynamicRouteConfig(dynamicRouteEntry.getName(), dynamicRouteEntry.getPath(), dynamicClientConfigs));
    }
    return dynamicRouteconfigs;
  }

  private ArrayList<DynamicClientConfig> createDynamicClientConfigs(DynamicRouteEntry dynamicRouteEntry) {
    ArrayList<DynamicClientConfig> dynamicClientConfigs = new ArrayList<>();
    for (String clientIp : dynamicRouteEntry.getClientsIps()) {
      dynamicClientConfigs.add(new DynamicClientConfig(dynamicRouteEntry.getClientName(), clientIp, dynamicRouteEntry.getPort(), dynamicRouteEntry.isTlsEnabled()));
    }
    return dynamicClientConfigs;
  }
}
