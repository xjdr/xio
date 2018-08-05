package com.xjeffrose.xio.config;

import java.util.ArrayList;

public class DynamicRouteConfig {
  private String name;
  private String path;
  private ArrayList<DynamicClientConfig> clientConfigs;

  public DynamicRouteConfig(String name, String path, ArrayList<DynamicClientConfig> clientConfigs) {
    this.name = name;
    this.path = path;
    this.clientConfigs = clientConfigs;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public ArrayList<DynamicClientConfig> getClientConfigs() {
    return clientConfigs;
  }
}
