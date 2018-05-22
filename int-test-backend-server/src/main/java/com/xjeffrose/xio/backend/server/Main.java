package com.xjeffrose.xio.backend.server;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

  public static void main(String args[]) throws Exception {
    if (args.length < 3) {
      throw new RuntimeException("please specify server 'port', 'header-tag' and 'h2' arguments");
    }

    // header-tag might be the ip address of this host or any other information you
    // would like to use to identify the traffic served up by this host
    final int port = Integer.parseInt(args[0]);
    final String name = args[1];
    final Boolean h2Capable = Boolean.parseBoolean(args[2]);
    RestHandlers.setTag(name);

    log.warn("starting h2 capable:{} service:{} on port:{}", h2Capable, name, port);
    RestServer server = new RestServer(port, true, h2Capable);
    server.start();
  }
}
