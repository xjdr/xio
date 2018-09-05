package com.xjeffrose.xio.application;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.google.common.annotations.VisibleForTesting;
import com.xjeffrose.xio.bootstrap.ServerChannelConfiguration;
import com.xjeffrose.xio.bootstrap.XioServiceLocator;
import com.xjeffrose.xio.config.TracingConfig;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.filter.Http1FilterConfig;
import com.xjeffrose.xio.filter.IpFilterConfig;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.EventLoopGroup;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;

public class ApplicationState {

  @Accessors(fluent = true)
  @Getter
  private final ApplicationConfig config;

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

  public ApplicationState(ApplicationConfig config, XioTracing tracing) {
    this.config = config;
    this.tracing = tracing;
    this.metricRegistry = new MetricRegistry();
    JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
    jmxReporter.start();
    this.channelConfiguration = config.serverChannelConfig();
    this.ipFilterConfig = new AtomicReference<>(new IpFilterConfig());
    this.http1FilterConfig = new AtomicReference<>(new Http1FilterConfig());
  }

  @VisibleForTesting
  public ApplicationState(
      ApplicationConfig config, Function<TracingConfig, XioTracing> tracingSupplier) {
    this(config, tracingSupplier.apply(config.getTracingConfig()));
  }

  public ApplicationState(ApplicationConfig config) {
    this(config, new XioTracing(config.getTracingConfig()));
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

  // TODO(br): Remove this. Objects should be injected with the ZkClient from XioServiceLocator instead of getting it from here.
  public ZkClient getZkClient() {
    return XioServiceLocator.getInstance().getZkClient();
  }
}
