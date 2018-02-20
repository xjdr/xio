package com.xjeffrose.xio.application;

import com.xjeffrose.xio.config.Configurator;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerInstrumentation;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

// TODO(CK): Application<S extends ApplicationState> ???
@Slf4j
public class Application implements AutoCloseable {

  // TOOD(CK): move this into ApplicationState
  @Getter private final ApplicationConfig config;

  // TODO(CK): move this into ApplicationState
  private final Map<String, XioServer> servers;

  @Getter private final ApplicationState state;

  // TODO(CK): move this into ApplicationState
  private final Configurator configurator;

  public Application(
      ApplicationConfig config,
      Map<String, XioServer> servers,
      ApplicationState state,
      Configurator configurator) {
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
