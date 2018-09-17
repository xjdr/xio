package com.xjeffrose.xio.exhibitor;

import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
  public static void main(String args[]) throws Exception {
    Application application =
        new ApplicationBootstrap("exhibitor-test")
            .addServer("main", bs -> bs.addToPipeline(new SmartHttpPipeline()))
            .build();

    application
        .getState()
        .getZkClient()
        .getClient()
        .create()
        .creatingParentsIfNeeded()
        .forPath("/test/key", "value".getBytes());
  }
}
