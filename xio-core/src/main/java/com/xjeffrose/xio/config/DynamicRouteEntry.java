package com.xjeffrose.xio.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class is the POJO representation of the data elements inside the route.json file This is
 * used as the base input for RouteReloading. It is later transformed into a
 * List<DynamicRouteConfig>
 */
@Getter
@Setter
@NoArgsConstructor
public class DynamicRouteEntry {
  @JsonProperty("name")
  private String name;

  @JsonProperty("path")
  private String path;

  @JsonProperty("client_name")
  private String clientName;

  @JsonProperty("ip_addresses")
  private List<String> clientsIps;

  @JsonProperty("port_number")
  private int port;

  @JsonProperty("tls_enabled")
  private boolean tlsEnabled;
}
