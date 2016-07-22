package com.xjeffrose.xio.server.trailhead;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main {

  static public void main(String[] args) {
    Server server = new Server();
    Config config = ConfigFactory.load("trailhead.conf");
    server.configure(config);
    server.start();
    System.out.println("WE BLOCKING");
    // TODO wait for kill signal
  }

}
