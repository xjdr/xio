package com.xjeffrose.xio.application;

import com.xjeffrose.xio.core.ZkClient;
import lombok.Getter;

public class XioServiceLocator {
  public static XioServiceLocator INSTANCE = null;

  private ApplicationConfig applicationConfig;
  private ApplicationState applicationState;

  public XioServiceLocator(ApplicationConfig applicationConfig, ApplicationState applicationState) {
    this.applicationConfig = applicationConfig;
    this.applicationState = applicationState;
  }

  public ApplicationConfig getApplicationConfig() {
    return applicationConfig;
  }

  public ApplicationState getApplicationState() {
    return applicationState;
  }

  @Getter(lazy = true)
  private final ZkClient zkClient = ZkClient.buildZkClient(applicationConfig);
}
