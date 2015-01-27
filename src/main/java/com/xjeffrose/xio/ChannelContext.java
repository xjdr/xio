package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

// TODO: On HTTP GET method stop reading after \r\n\r\n
// TODO: Parse JSON

abstract class ChannelContext {
  private static final Logger log = Log.getLogger(ChannelContext.class.getName());

  public final SocketChannel channel;
  public final ByteBuffer bb = ByteBuffer.allocateDirect(1024);
  public final HttpParser parser = new HttpParser();
  public final HttpRequest req = new HttpRequest();
  public final HttpResponse resp = new HttpResponse();

  private Service service;

  ChannelContext(SocketChannel channel) {
    this.channel = channel;
  }

  private enum State {
    got_request,
    start_parse,
    finished_parse,
    start_response,
    finished_response,
  };

  State state = State.got_request;

  abstract public void read();

  public void write() {
    try {
      if(state == State.finished_parse) {
        state = State.start_response;
        channel.write(resp.get());
        channel.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    state = State.finished_response;
  }
}
