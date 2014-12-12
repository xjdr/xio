package com.xjeffrose.xio;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.*;

class GateKeeper implements Event, Acceptable {
  private static final Logger log = com.xjeffrose.log.Log.create();

  private final Acceptor acceptor;
  private final List<Filter> filters;
  private final ExecutorService esx;
  private EventLoop loop;

  GateKeeper(int port, List<Filter> filters) throws IOException {
    acceptor = new Acceptor(port);
    this.filters = filters;
    esx = Executors.newFixedThreadPool(12); // TODO: Move this to a better home
  }

  @Override public void registerWithEventLoop(EventLoop loop) throws IOException {
    acceptor.registerWithSelector(loop.getSelector(), this);
    this.loop = loop;
  }

  @Override public void doAccept() throws IOException {
    Connection client = acceptor.accept();
    log.info("accepted a connection from " + client);

    boolean good = filters.stream()
                          .map(f -> f.apply(client))
                          .allMatch(b -> b.booleanValue() == true);

    if (good) {
      //loop.register(client); // TODO: register this client with a different EventLoop
      esx.submit(new Terminator(client));
    } else {
      log.info("dropped a bad connection from " + client);
      client.close();
    }
  }

}
