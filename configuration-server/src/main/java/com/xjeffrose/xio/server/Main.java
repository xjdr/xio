package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.config.Configurator;
import org.apache.curator.test.TestingServer;

public class Main {

  TestingServer zkServer;
  Configurator server;

  public Main() throws Exception {
    zkServer = new TestingServer(2181, true);

    Config override = ConfigFactory.parseString("zookeeper { cluster = \"" + zkServer.getConnectString() + "\" }, configurationUpdateServer { enabled = true }");
    server = Configurator.build(override.withFallback(ConfigFactory.load().getConfig("xio.exampleApplication.settings")));
  }

  public void run() throws Exception {
    server.start();
  }

  public static void main(String[] args) throws Exception {
    new Main().run();
  }

}
