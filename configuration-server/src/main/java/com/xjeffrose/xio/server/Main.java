package com.xjeffrose.xio.server;

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.config.Configurator;
import org.apache.curator.test.TestingServer;

public class Main {

  TestingServer zkServer;
  Configurator server;

  public Main() throws Exception {
    zkServer = new TestingServer(2181, true);

    server = Configurator.build(ConfigFactory.load().getConfig("xio.exampleApplication.settings"));
  }

  public void run() throws Exception {
    server.start();
  }

  public static void main(String[] args) throws Exception {
    new Main().run();
  }
}
