package com.xjeffrose.xio.application;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ServerChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.filter.Http1FilterConfig;
import com.xjeffrose.xio.filter.IpFilterConfig;
import com.xjeffrose.xio.http.ClientState;
import com.xjeffrose.xio.http.DefaultRouter;
import com.xjeffrose.xio.http.Router;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.EventLoopGroup;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.experimental.Accessors;

public class ApplicationState {

  @Accessors(fluent = true)
  @Getter
  private final ApplicationConfig config;

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

  private final AtomicReference<Router> router;

  public ApplicationState(ApplicationConfig config) {
    this.config = config;
    this.tracing = config.getTracing();
    this.metricRegistry = new MetricRegistry();

    zkClient = config.zookeeperClient();
    channelConfiguration = config.serverChannelConfig();

    ipFilterConfig = new AtomicReference<>(new IpFilterConfig());
    zkClient.registerUpdater(
        new IpFilterConfig.Updater(config.getIpFilterPath(), this::setIpFilterConfig));

    http1FilterConfig = new AtomicReference<>(new Http1FilterConfig());
    zkClient.registerUpdater(
        new Http1FilterConfig.Updater(config.getHttp1FilterPath(), this::setHttp1FilterConfig));

    // Set this to null.  If router is being bootstrapped from here, it will check for null before
    // proceeding.
    // It is expected that dynamic configuration service will update this as needed.
    router = new AtomicReference<>(new DefaultRouter());
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

  public Router getRouter() {
    return router.get();
  }

  public void setRouter(Router router) {
    this.router.set(router);
  }

  public ClientState createClientState(
      ClientChannelConfiguration channelConfig, ClientConfig config) {
    return new ClientState(channelConfig, config);
  }

  public ClientState createClientState(ClientConfig config) {
    return createClientState(ChannelConfiguration.clientConfig(workerGroup()), config);
  }
}
