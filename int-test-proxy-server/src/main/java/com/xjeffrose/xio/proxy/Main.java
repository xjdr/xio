package com.xjeffrose.xio.proxy;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      throw new RuntimeException(
          "please specify arguments for reverse proxy server: \n"
              + "name \n"
              + "config file path \n"
              + "proxy config key \n"
              + "route config key(s) (comma delimited) - optional, default = 'xio.testProxyRoute'");
    }

    String name = args[0];
    log.debug("starting proxy server named: {}", name);
    String configPath = args[1];
    String proxyConfig = args[2];
    final String routConfig;
    if (args.length > 3) {
      routConfig = args[3];
    } else {
      routConfig = "xio.testProxyRoute";
    }
    Config config = ConfigFactory.load(ConfigFactory.parseFile(new File(configPath)));
    new ReverseProxyServer(proxyConfig, routConfig).start(config);
    log.debug("proxy accepting connections");
  }
}
