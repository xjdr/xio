package com.xjeffrose.xio.http;

import com.xjeffrose.xio.server.Route;
import com.typesafe.config.Config;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.List;
import java.net.InetSocketAddress;
import com.google.common.collect.ImmutableList;
import io.netty.handler.codec.http.HttpRequest;

public class RoundRobinProxyConfig {

  public static class Host {

    public final InetSocketAddress address;
    public final String hostHeader;
    public final boolean needSSL;

    public Host(InetSocketAddress address, String hostHeader, boolean needSSL) {
      this.address = address;
      this.hostHeader = hostHeader;
      this.needSSL = needSSL;
    }

  }

  private final AtomicInteger next = new AtomicInteger();
  private final ImmutableList<Host> hosts;

  public RoundRobinProxyConfig(ImmutableList<Host> hosts) {
    this.hosts = hosts;
  }

  private static ImmutableList<Host> parse(Config config) {
    List<Host> hosts = config.root().entrySet().stream().map((item) -> {
      Config entry = config.getConfig(item.getKey());
      InetSocketAddress address = new InetSocketAddress(entry.getString("host"), entry.getInt("port"));
      String hostHeader = entry.getString("hostHeader");
      boolean needSSL = entry.getBoolean("needSSL");

      return new Host(address, hostHeader, needSSL);
    }).collect(Collectors.toList());

    return ImmutableList.copyOf(hosts);
  }

  public RoundRobinProxyConfig(Config config) {
    this(parse(config.getConfig("proxy.hosts")));
  }

  public static RoundRobinProxyConfig fromConfig(String key, Config config) {
    return new RoundRobinProxyConfig(config.getConfig(key));
  }

  public RouteProvider getRouteProvider(HttpRequest request) {
    int idx = next.getAndIncrement();
    Host host = hosts.get(idx % hosts.size());
    ProxyConfig proxyConfig = new ProxyConfig(host.address, host.hostHeader, "", request.uri(), host.needSSL);
    return new SimpleProxyRoute(Route.build("/:*path"), proxyConfig);
  }

}
