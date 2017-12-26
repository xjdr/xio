package com.xjeffrose.xio.http;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class ProxyConfig {

  public final InetSocketAddress address;
  public final String hostHeader;
  public final String url;
  public final String urlPath;
  public final boolean needSSL;
  public final boolean pathPassthru;

  public ProxyConfig(
      InetSocketAddress address,
      String hostHeader,
      String url,
      String urlPath,
      boolean needSSL,
      boolean pathPassthru) {
    this.address = address;
    this.hostHeader = hostHeader;
    this.url = url;
    this.urlPath = urlPath;
    this.needSSL = needSSL;
    this.pathPassthru = pathPassthru;
  }

  public static ProxyConfig parse(String url) {
    return parse(url, false);
  }

  public static ProxyConfig parse(String url, boolean pathPassthru) {
    int defaultPort = -1;
    boolean needSSL = false;
    boolean standardPort = false;
    try {
      URL parsedUrl = new URL(url);
      String host = parsedUrl.getHost();
      int port = parsedUrl.getPort();
      String urlPath = parsedUrl.getPath();

      if (parsedUrl.getProtocol().equals("http")) {
        defaultPort = 80;
      } else if (parsedUrl.getProtocol().equals("https")) {
        defaultPort = 443;
        needSSL = true;
      }

      if (port == -1) {
        if (defaultPort == -1) {
          throw new RuntimeException("Invalid port");
        } else {
          port = defaultPort;
          standardPort = true;
        }
      }

      String hostHeader = host;
      if (!standardPort) {
        hostHeader += ":" + port;
      }

      InetSocketAddress address = new InetSocketAddress(host, port);
      return new ProxyConfig(address, hostHeader, url, urlPath, needSSL, pathPassthru);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
