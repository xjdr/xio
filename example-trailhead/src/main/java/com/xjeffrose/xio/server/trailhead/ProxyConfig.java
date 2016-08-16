package com.xjeffrose.xio.server.trailhead;

import java.net.InetSocketAddress;

public class ProxyConfig {

  InetSocketAddress address;
  String hostHeader;
  int port;
  String url;
  String urlPath;
  boolean needSSL;

  public ProxyConfig(InetSocketAddress address, String hostHeader, String url, String urlPath, boolean needSSL) {
    this.address = address;
    this.hostHeader = hostHeader;
    this.url = url;
    this.urlPath = urlPath;
    this.needSSL = needSSL;
  }

}
