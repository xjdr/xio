package com.xjeffrose.xio.config;

import com.google.gson.annotations.SerializedName;

/**
 * This class is the POJO representation of the data elements inside the route.json file
 * This is used as the base input for RouteReloading. It is later transformed into a List<DynamicRouteConfig>
 */
public class DynamicRouteEntry {
  @SerializedName("name")
  private String name;

  @SerializedName("path")
  private String path;

  @SerializedName("client_name")
  private String clientName;

  @SerializedName("ip_addresses")
  private String[] clientsIps;

  @SerializedName("port_number")
  private int port;

  @SerializedName("tls_enabled")
  private boolean tlsEnabled;

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public String getClientName() {
    return clientName;
  }

  public String[] getClientsIps() {
    if (clientsIps == null) {
      return new String[] {};
    }
    return clientsIps;
  }

  public int getPort() {
    return port;
  }

  public boolean isTlsEnabled() {
    return tlsEnabled;
  }
}
