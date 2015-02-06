package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class ClientChannelContext extends ChannelContext {
  private static final Logger log = Log.getLogger(ClientChannelContext.class.getName());

  /* public final HttpRequest request = new HttpRequest(); */
  public final HttpResponse response = new HttpResponse();
  public final HttpResponseParser parser = new HttpResponseParser();
  public final Promise<HttpResponse> promise = new Promise<HttpResponse>();

  private final ByteBuffer[] req;

  private enum State {
    start_request,
    start_receive,
    parse_receive,
  };

  State state = State.start_request;

  ClientChannelContext(SocketChannel channel, ByteBuffer[] req) {
    super(channel);
    this.req = req;
  }

  public void read() {
    int nread = 1;

    while (nread > 0) {
      try {
        nread = channel.read(response.responseBuffer);
        if (nread == 0) {
          break;
        }
        if (!parser.parse(response)) {
          throw new RuntimeException("Parser Failed to Parse");
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (nread == -1) {
        try {
          //log.info("Closing Channel " + channel);
          channel.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public void write() {
    try {
      if (state == State.start_request) {
        channel.write(req);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
