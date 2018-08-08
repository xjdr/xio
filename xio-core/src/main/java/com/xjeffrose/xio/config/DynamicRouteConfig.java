package com.xjeffrose.xio.config;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** This class is the POJO representation of the input data that maps to a RouteConfig/RouteState */
@EqualsAndHashCode
@RequiredArgsConstructor
@Getter
public class DynamicRouteConfig {
  private final String name;
  private final String path;
  private final List<DynamicClientConfig> clientConfigs;
}
