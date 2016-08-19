package com.xjeffrose.xio.server;

import com.xjeffrose.xio.config.Configurator;
import org.apache.curator.test.TestingServer;

public class Main {

  TestingServer zkServer;
  Configurator server;

  public Main() throws Exception {
    zkServer = new TestingServer(2181, true);

    server = Configurator.build(zkServer.getConnectString());
  }

  public void run() throws Exception {
    server.start();
  }

  public static void main(String[] args) throws Exception {
    new Main().run();
  }

}
