package com.xjeffrose.xio.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** This class is a POJO representation of the input data that maps to a ClientConfig */
@EqualsAndHashCode
@RequiredArgsConstructor
@Getter
public class DynamicClientConfig {
  private final String ipAddress;
  private final int port;
  private final boolean tlsEnabled;
}
