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

  public ProxyConfig(InetSocketAddress address, String hostHeader, String url, String urlPath, boolean needSSL) {
    this.address = address;
    this.hostHeader = hostHeader;
    this.url = url;
    this.urlPath = urlPath;
    this.needSSL = needSSL;
  }

  public static ProxyConfig parse(String url) {
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
      return new ProxyConfig(address, hostHeader, url, urlPath, needSSL);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

}
