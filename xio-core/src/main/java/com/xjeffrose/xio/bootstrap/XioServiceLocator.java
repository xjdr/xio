package com.xjeffrose.xio.bootstrap;

import com.google.common.base.Preconditions;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.core.ZkClient;
import lombok.Getter;

public class XioServiceLocator {
  private static XioServiceLocator instance = null;

  private ApplicationConfig applicationConfig;
  private ApplicationState applicationState;

  private XioServiceLocator(ApplicationConfig applicationConfig, ApplicationState applicationState) {
    this.applicationConfig = applicationConfig;
    this.applicationState = applicationState;
  }

  public static XioServiceLocator getInstance() {
    Preconditions.checkNotNull(instance, "XioServiceLocator is created by ApplicationBootstrap during it's constructor. Make sure that an ApplicationBootstrap has been created before accessing XioServiceLocator.");
    return instance;
  }

  /**
   * This is how ApplicationBootstrap initializes the shared instance of XioServiceLocator.
   * The above statement is why this function is package private and XioServiceLocator is in the bootstrap package.
   * */
  static void buildInstance(ApplicationConfig applicationConfig, ApplicationState applicationState) {
    XioServiceLocator.instance = new XioServiceLocator(applicationConfig, applicationState);
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
