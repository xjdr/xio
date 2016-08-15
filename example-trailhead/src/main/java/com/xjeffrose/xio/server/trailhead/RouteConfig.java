package com.xjeffrose.xio.server.trailhead;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.xjeffrose.xio.server.Route;
import java.util.LinkedHashMap;
import java.util.Map;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
public class RouteConfig {

  static public class ProxyTo {
    InetSocketAddress address;
    String host;
    String url;
    String urlPath;
    boolean needSSL;

    public ProxyTo(InetSocketAddress address, String host, String url, String urlPath, boolean needSSL) {
      this.address = address;
      this.host = host;
      this.url = url;
      this.urlPath = urlPath;
      this.needSSL = needSSL;
    }
  }


  Map<Route, ProxyTo> routes = new LinkedHashMap<>();


  public RouteConfig() {
  }

  ImmutableMap<Route, ProxyTo> copy() {
    return ImmutableMap.copyOf(routes);
  }

  public RouteConfig(Config config) {
    config.root().entrySet().stream().forEach((item) -> {
      Config entry = config.getConfig(item.getKey());
      String url = entry.getString("url");
      String urlPath;
      String host;
      int port;
      int defaultPort;
      boolean needSSL = false;
      try {
        URL parsedUrl = new URL(url);
        host = parsedUrl.getHost();
        port = parsedUrl.getPort();
        urlPath = parsedUrl.getPath();
        if (parsedUrl.getProtocol().equals("http")) {
          defaultPort = 80;
        } else if (parsedUrl.getProtocol().equals("https")) {
          defaultPort = 443;
          needSSL = true;
        } else {
          defaultPort = -1;
        }
        if (port == -1) {
          if (defaultPort == -1) {
            throw new RuntimeException("Invalid port");
          } else {
            port = defaultPort;
          }
        }
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
      System.out.println("Entry: " + entry);

      InetSocketAddress address = new InetSocketAddress(host, port);
      ProxyTo proxyTo = new ProxyTo(address, host, url, urlPath, needSSL);
      routes.put(Route.build(item.getKey()), proxyTo);
    });
  }

  static public RouteConfig fromConfig(String key, Config config) {
    return new RouteConfig(config.getConfig(key));
  }
}
