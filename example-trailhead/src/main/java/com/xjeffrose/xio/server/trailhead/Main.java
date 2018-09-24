package com.xjeffrose.xio.server.trailhead;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main {

  public static void main(String[] args) {
    Config config = ConfigFactory.load("trailhead.conf");
    Trailhead application = new Trailhead(config);
    application.start();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                application.close();
              }
            });
  }
}
