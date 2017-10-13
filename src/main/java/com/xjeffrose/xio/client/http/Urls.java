package com.xjeffrose.xio.client.http;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Urls {

  private Urls() {}

  public static int getEffectivePort(URL url) {
    int specifiedPort = url.getPort();
    return specifiedPort != -1
      ? specifiedPort
      : getDefaultPort(url.getProtocol());
  }

  public static int getDefaultPort(String protocol) {
    if ("http".equals(protocol)) return 80;
    if ("https".equals(protocol)) return 443;
    return -1;
  }

  public static InetSocketAddress getEndpoint(URL url) {
    int port = getEffectivePort(url);
    if (port == -1) {
      throw new RuntimeException("Invalid port for url '" + url + "'");
    }

    return new InetSocketAddress(url.getHost(), port);
  }

  public static String getHostHeader(URL url) {
    if (getEffectivePort(url) == getDefaultPort(url.getProtocol())) {
      return url.getHost();
    }
    return url.getHost() + ":" + getEffectivePort(url);
  }

  public static URL parse(String raw) {
    try {
      return new URL(raw);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
