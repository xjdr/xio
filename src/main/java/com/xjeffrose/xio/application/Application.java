package com.xjeffrose.xio.application;

import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerInstrumentation;
import lombok.Getter;
import lombok.extern.log4j.Log4j;

import java.util.Map;

@Log4j
public class Application implements AutoCloseable {

  private final Map<String, XioServer> servers;

  @Getter
  private final ApplicationState state;

  public Application(Map<String, XioServer> servers, ApplicationState state) {
    this.servers = servers;
    this.state = state;
  }

  public XioServerInstrumentation instrumentation(String server) {
    return servers.get(server).instrumentation();
  }

  public void close() {
    log.debug("Closing " + this);
    servers.values().stream().forEach((v) -> v.close());
  }

}
