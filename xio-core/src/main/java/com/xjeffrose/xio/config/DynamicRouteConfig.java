package com.xjeffrose.xio.config;

import java.util.List;
import lombok.EqualsAndHashCode;

/** This class is the POJO representation of the input data that maps to a RouteConfig/RouteState */
@EqualsAndHashCode
public class DynamicRouteConfig {
  private String name;
  private String path;
  private List<DynamicClientConfig> clientConfigs;

  public DynamicRouteConfig(String name, String path, List<DynamicClientConfig> clientConfigs) {
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

  public List<DynamicClientConfig> getClientConfigs() {
    return clientConfigs;
  }
}
