package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

// TODO: On HTTP GET method stop reading after \r\n\r\n
// TODO: Parse JSON

class ServerChannelContext extends ChannelContext {
  private static final Logger log = Log.getLogger(ServerChannelContext.class.getName());

  public final HttpParser parser = new HttpParser();
  public final HttpRequest req = new HttpRequest();
  public final HttpResponse resp = new HttpResponse();

  private final Map<String, Service> routes;
  private Service service;

  ServerChannelContext(SocketChannel channel, Map<String, Service> routes) {
    super(channel);
    this.routes = routes;
  }

  private enum State {
    got_request,
    start_parse,
    finished_parse,
    start_response,
    finished_response,
  };

  State state = State.got_request;

  public void read() {
    int nread = 1;

    while (nread > 0) {
      try {
        nread = channel.read(bb);
        if (nread == 0) {
          break;
        }
        state = State.start_parse;
        if (!parser.parse(req, bb)) {
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
    state = State.finished_parse;
    handleReq();
  }

  private void handleReq() {
    final String uri = req.uri();
    if (state == State.finished_parse && routes.containsKey(uri)) {
      service = routes.get(uri);
      service.handle(req, resp);
    }
  }

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
