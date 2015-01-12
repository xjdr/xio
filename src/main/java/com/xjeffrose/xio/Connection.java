package com.xjeffrose.xio;

import java.nio.*;
import java.nio.channels.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Connection {
  private static final Logger log = Log.getLogger(Connection.class.getName());
  public final SocketChannel channel;

  Connection(SocketChannel channel) {
    this.channel = channel;
    log.info("Channel Context Created");
  }

  public void write() {}

  public void read() {}

}
