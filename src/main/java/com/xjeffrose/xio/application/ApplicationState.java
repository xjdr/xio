package com.xjeffrose.xio.application;

import com.xjeffrose.xio.bootstrap.ServerChannelConfiguration;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.filter.Http1FilterConfig;
import com.xjeffrose.xio.filter.IpFilterConfig;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;

public class ApplicationState {

  @Getter
  private final ZkClient zkClient;

  @Getter
  private final ServerChannelConfiguration channelConfiguration;

  // TODO(CK): store ClientChannelConfiguration here as well

  private final AtomicReference<IpFilterConfig> ipFilterConfig;

  private final AtomicReference<Http1FilterConfig> http1FilterConfig;

  public ApplicationState(ApplicationConfig config) {
    channelConfiguration = config.serverChannelConfig();
    zkClient = config.zookeeperClient();

    ipFilterConfig = new AtomicReference<IpFilterConfig>(new IpFilterConfig());
    zkClient.registerUpdater(new IpFilterConfig.Updater(config.getIpFilterPath(), this::setIpFilterConfig));

    http1FilterConfig = new AtomicReference<Http1FilterConfig>(new Http1FilterConfig());
    zkClient.registerUpdater(new Http1FilterConfig.Updater(config.getHttp1FilterPath(), this::setHttp1FilterConfig));
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

}
