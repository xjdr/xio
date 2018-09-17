package com.xjeffrose.xio.server.trailhead;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.xjeffrose.xio.http.ProxyConfig;
import com.xjeffrose.xio.server.Route;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class RouteConfig {

  Map<Route, ProxyConfig> routes = new LinkedHashMap<>();

  public RouteConfig() {}

  ImmutableMap<Route, ProxyConfig> copy() {
    return ImmutableMap.copyOf(routes);
  }

  public RouteConfig(Config config) {
    config
        .root()
        .entrySet()
        .stream()
        .forEach(
            (item) -> {
              Config entry = config.getConfig(item.getKey());
              String url = entry.getString("url");
              String urlPath;
              String host;
              int port;
              int defaultPort;
              boolean needSSL = false;
              boolean standardPort = false;
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
                    standardPort = true;
                  }
                }
              } catch (MalformedURLException e) {
                throw new RuntimeException(e);
              }

              String hostHeader = host;
              if (!standardPort) {
                hostHeader += ":" + port;
              }

              InetSocketAddress address = new InetSocketAddress(host, port);
              ProxyConfig proxyConfig = new ProxyConfig(address, hostHeader, url, urlPath, needSSL);
              routes.put(Route.build(item.getKey()), proxyConfig);
            });
  }

  public static RouteConfig fromConfig(String key, Config config) {
    return new RouteConfig(config.getConfig(key));
  }
}
