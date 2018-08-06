package com.xjeffrose.xio.config;

import lombok.EqualsAndHashCode;

/** This class is a POJO representation of the input data that maps to a ClientConfig */
@EqualsAndHashCode
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
}
