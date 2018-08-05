package com.xjeffrose.xio.config;

import java.util.Objects;

/** This class is a POJO representation of the input data that maps to a ClientConfig */
public class DynamicClientConfig {
  private String clientName;
  private String ipAddress;
  private int port;
  private boolean tlsEnabled;

  public DynamicClientConfig(String clientName, String ipAddress, int port, boolean tlsEnabled) {
    this.clientName = clientName;
    this.ipAddress = ipAddress;
    this.port = port;
    this.tlsEnabled = tlsEnabled;
  }

  public String getClientName() {
    return clientName;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public int getPort() {
    return port;
  }

  public boolean isTlsEnabled() {
    return tlsEnabled;
  }

  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof DynamicClientConfig)) {
      return false;
    }
    DynamicClientConfig occ = (DynamicClientConfig) other;
    return Objects.equals(occ.clientName, clientName)
        && Objects.equals(occ.ipAddress, ipAddress)
        && occ.port == port
        && occ.tlsEnabled == tlsEnabled;
  }

  public int hashCode() {
    return Objects.hash(clientName, ipAddress, port, tlsEnabled);
  }
}
