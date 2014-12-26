package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.logging.*;
import java.util.stream.*;


import com.xjeffrose.log.*;

class Session {
  private static final Logger log = Log.getLogger(Session.class.getName());
  private final ChannelContext ctx;
  private final SocketChannel channel;
  private int nread;

  Session (SocketChannel channel) {
    this.channel = channel;
    this.ctx = new ChannelContext(channel);
    /* read(); */
    InputStream stream = new InputStream(ctx);
    Thread inStream = new Thread(stream); // Should be a Future
    inStream.start();
    try {
      Thread.sleep(200);
    } catch (Exception e) {}
    process();
  }

  private void read() {
    try {
      nread = ctx.channel.read(ctx.inBuf);
      if (nread == -1) {
        ctx.channel.close();
      }
    } catch (IOException e) {}
  }

  private void process() {

    String raw = new String(ctx.inBuf.array(), Charset.forName("UTF-8"));
    String[] parts = raw.split("\r\n\r\n");
    String[] headers = parts[0].split("\r\n");
    log.info(headers[0]);
  }


}
