package com.xjeffrose.xio.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

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

  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof DynamicRouteConfig)) {
      return false;
    }

    DynamicRouteConfig drc = (DynamicRouteConfig) other;
    return drc.name.equals(name) &&
           drc.path.equals(path) &&
           Arrays.equals(drc.clientConfigs.toArray(), clientConfigs.toArray());
  }

  public int hashCode() {
    return Objects.hash(name, path) + clientConfigs.toArray().hashCode();
  }

}
