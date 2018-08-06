package com.xjeffrose.xio.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is the POJO representation of the data elements inside the route.json file This is
 * used as the base input for RouteReloading. It is later transformed into a
 * List<DynamicRouteConfig>
 */
public class DynamicRouteEntry {
  @JsonProperty("name")
  private String name;

  @JsonProperty("path")
  private String path;

  @JsonProperty("client_name")
  private String clientName;

  @JsonProperty("ip_addresses")
  private String[] clientsIps;

  @JsonProperty("port_number")
  private int port;

  @JsonProperty("tls_enabled")
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
