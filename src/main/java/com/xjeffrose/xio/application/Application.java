package com.xjeffrose.xio.application;

import com.xjeffrose.xio.config.Configurator;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerInstrumentation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class Application implements AutoCloseable {

  @Getter
  private final ApplicationConfig config;

  private final Map<String, XioServer> servers;

  @Getter
  private final ApplicationState state;

  private final Configurator configurator;

  public Application(ApplicationConfig config, Map<String, XioServer> servers, ApplicationState state, Configurator configurator) {
    this.config = config;
    this.servers = servers;
    this.state = state;
    this.configurator = configurator;
  }

  public XioServerInstrumentation instrumentation(String server) {
    return servers.get(server).getInstrumentation();
  }

  public void close() {
    log.debug("Closing " + this);
    servers.values().stream().forEach((v) -> v.close());
    configurator.close();
  }

}
