package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class ChannelContext {
  private static final Logger log = Log.getLogger(ChannelContext.class.getName());

  public final SocketChannel channel;
  public final ByteBuffer inBuf = ByteBuffer.allocate(1024);
  public final ByteBuffer outBuf = ByteBuffer.allocate(1024);

  ChannelContext(SocketChannel channel) {
    this.channel = channel;

    log.info("ChannelContext Created");
  }

  public void read() {
    int nread = 1;
    while (nread > 0) {
      try {
        nread = channel.read(inBuf);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (nread == -1) {
        try {
          log.info("Closing Channel " + channel);
          channel.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    String raw = new String(inBuf.array(), Charset.forName("UTF-8"));
    String[] parts = raw.split("\r\n\r\n");
    String[] headers = parts[0].split("\r\n");
    log.info(headers[0]);

  }

  public void write() {
  }

}
