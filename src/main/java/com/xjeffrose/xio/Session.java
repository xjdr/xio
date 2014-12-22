package com.xjeffrose.xio;

import java.nio.channels.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Session {
  private static final Logger log = Log.getLogger(Session.class.getName());
  private SocketChannel channel;

  public Session (SocketChannel channel) {
    this.channel = channel;

    log.info("Creating Session");
  }

}
