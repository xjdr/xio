package com.xjeffrose.xio.application;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ServerChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.filter.Http1FilterConfig;
import com.xjeffrose.xio.filter.IpFilterConfig;
import com.xjeffrose.xio.http.ClientState;
import com.xjeffrose.xio.http.ProxyClientFactory;
import com.xjeffrose.xio.http.RouteState;
import com.xjeffrose.xio.server.RouteStates;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.EventLoopGroup;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.experimental.Accessors;

public class ApplicationState {

  @Accessors(fluent = true)
  @Getter
  private final ApplicationConfig config;

  protected AtomicReference<RouteStates> routeStatesRef;
  protected final ProxyClientFactory clientFactory;

  @Getter private final ZkClient zkClient;

  @Getter private final ServerChannelConfiguration channelConfiguration;

  // TODO(CK): store ClientChannelConfiguration here as well

  @Accessors(fluent = true)
  @Getter
  private final XioTracing tracing;

  @Getter private MetricRegistry metricRegistry;

  @VisibleForTesting
  public void setMetricRegistry(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  private final AtomicReference<IpFilterConfig> ipFilterConfig;

  private final AtomicReference<Http1FilterConfig> http1FilterConfig;

  public ApplicationState(ApplicationConfig config) {
    this.config = config;
    this.tracing = config.getTracing();
    this.metricRegistry = new MetricRegistry();
    JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
    jmxReporter.start();

    zkClient = config.zookeeperClient();
    channelConfiguration = config.serverChannelConfig();

    ipFilterConfig = new AtomicReference<>(new IpFilterConfig());
    zkClient.registerUpdater(
        new IpFilterConfig.Updater(config.getIpFilterPath(), this::setIpFilterConfig));

    http1FilterConfig = new AtomicReference<>(new Http1FilterConfig());
    zkClient.registerUpdater(
        new Http1FilterConfig.Updater(config.getHttp1FilterPath(), this::setHttp1FilterConfig));

    routeStatesRef = new AtomicReference<>();
    clientFactory = new ProxyClientFactory(this);
  }

  public EventLoopGroup workerGroup() {
    return channelConfiguration.workerGroup();
  }

  public IpFilterConfig getIpFilterConfig() {
    return ipFilterConfig.get();
  }

  public void setIpFilterConfig(IpFilterConfig newConfig) {
    ipFilterConfig.set(newConfig);
  }

  public Http1FilterConfig getHttp1FilterConfig() {
    return http1FilterConfig.get();
  }

  public void setHttp1FilterConfig(Http1FilterConfig newConfig) {
    http1FilterConfig.set(newConfig);
  }

  public void reloadConfig(ApplicationConfig previousConfig, ApplicationConfig updatedConfig) {
    // TODO(JL): Need to account for other reload configurations besides proxy routes
    //super(newAppConfig);

    if (!updatedConfig.proxyRoutes().equals(previousConfig.proxyRoutes())) {
      routeStatesRef.set(new RouteStates(updatedConfig, this, clientFactory));
    }
  }

  public ImmutableMap<String, RouteState> routes() {
    return routeStatesRef.get().routeMap();
  }

  public ClientState createClientState(
      ClientChannelConfiguration channelConfig, ClientConfig config) {
    return new ClientState(channelConfig, config);
  }

  public ClientState createClientState(ClientConfig config) {
    return createClientState(ChannelConfiguration.clientConfig(workerGroup()), config);
  }
}
