package com.xjeffrose.xio.server;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XioServer implements AutoCloseable {

  private Channel serverChannel;
  private final XioServerInstrumentation instrumentation;

  public XioServer(Channel serverChannel, XioServerInstrumentation instrumentation) {
    this.serverChannel = serverChannel;
    this.instrumentation = instrumentation;
  }

  public void close() {
    log.debug("Closing " + this);
    serverChannel.close();
  }

  public XioServerInstrumentation instrumentation() {
    return instrumentation;
  }

  public Channel getServerChannel() {
    return serverChannel;
  }

}
