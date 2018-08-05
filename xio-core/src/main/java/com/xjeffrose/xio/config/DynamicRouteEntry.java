package com.xjeffrose.xio.config;

import com.google.gson.annotations.SerializedName;

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
      return new String[]{};
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
