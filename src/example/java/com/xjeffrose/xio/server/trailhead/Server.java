package com.xjeffrose.xio.server.trailhead;

import com.typesafe.config.Config;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.pipeline.XioHttp1_1Pipeline;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;

public class Server implements AutoCloseable {

  private XioServerConfig serverConfig;
  private XioServerState serverState;
  private XioServerBootstrap bootstrap;
  private XioServer xioServer;
  private RouteConfig routes;

  public Server() {
  }

  public void configure(Config config) {
    serverConfig = XioServerConfig.fromConfig("trailhead.server", config);
    serverState = XioServerState.fromConfig("trailhead.application", config);
    routes = RouteConfig.fromConfig("trailhead.routes", config);
    bootstrap = new XioServerBootstrap(serverConfig, serverState)
      .addToPipeline(new XioHttp1_1Pipeline(new Http1ProxyFragment(routes)))
    ;
  }

  public void start() {
    xioServer = bootstrap.build();
    System.out.println("STARTING");
  }

  public void close() {
    xioServer.close();
  }

}
