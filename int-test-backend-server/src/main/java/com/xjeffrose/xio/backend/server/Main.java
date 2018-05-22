package com.xjeffrose.xio.backend.server;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

  public static void main(String args[]) throws Exception {
    if (args.length < 2) {
      throw new RuntimeException("please specify server 'port' and 'header-tag' arguments");
    }

    // header-tag might be the ip address of this host or any other information you
    // would like to use to identify the traffic served up by this host
    final int port = Integer.parseInt(args[0]);
    final String name = args[1];
    RestHandlers.setTag(name);

    log.warn("starting {} service on port: {}", name, port);
    RestServer server = new RestServer(port, true);
    server.start();
  }
}
