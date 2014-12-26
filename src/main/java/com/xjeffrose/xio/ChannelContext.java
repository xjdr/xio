package com.xjeffrose.xio;

/* import java.io.*; */
import java.nio.*;
import java.nio.channels.*;
/* import java.util.concurrent.*; */
import java.util.logging.*;

import com.xjeffrose.log.*;

class ChannelContext {
  private static final Logger log = Log.getLogger(ChannelContext.class.getName());
  public final SocketChannel channel;
  public final ByteBuffer inBuf = ByteBuffer.allocate(1024);

  ChannelContext(SocketChannel channel) {
    this.channel = channel;
    log.info("Channel Context Created");
  }

}
