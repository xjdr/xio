package com.xjeffrose.xio.server;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XioServer implements AutoCloseable {

  @Getter
  private final Channel serverChannel;

  @Getter
  private final XioServerInstrumentation instrumentation;

  @Getter
  private final XioServerConfig config;

  @Getter
  private final XioServerState state;

  public XioServer(Channel serverChannel, XioServerInstrumentation instrumentation, XioServerConfig config, XioServerState state) {
    this.serverChannel = serverChannel;
    this.instrumentation = instrumentation;
    this.config = config;
    this.state = state;
  }

  public void close() {
    log.debug("Closing " + this);
    serverChannel.close();
  }

  public int getPort() {
    return instrumentation.boundAddress().getPort();
  }

}
