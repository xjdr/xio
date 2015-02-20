package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class ClientChannelContext extends ChannelContext {
  private static final Logger log = Log.getLogger(ClientChannelContext.class.getName());

  public final ByteBuffer bb = ByteBuffer.allocateDirect(1024);
  public final HttpResponse response = new HttpResponse();
  public final HttpResponseParser parser = new HttpResponseParser(response);
  public final Promise<HttpResponse> promise = new Promise<HttpResponse>();

  private enum State {
    start_request,
    start_receive,
    parse_receive,
  };

  State state = State.start_request;

  ClientChannelContext(SocketChannel channel) {
    super(channel);
  }

  public void read() {
    int nread = 1;

    while (nread > 0) {
      try {
        nread = channel.read(response.buffer);
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

    //TODO: Do something upon read!
  }

  public void write() {
    try {
      if (state == State.start_request) {
        channel.write(HttpRequest.defaultRequest());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
