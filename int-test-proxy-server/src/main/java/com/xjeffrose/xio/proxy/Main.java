package com.xjeffrose.xio.proxy;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      throw new RuntimeException("please specify a name, config path and proxy config key");
    }
    String name = args[0];
    log.debug("starting proxy server named: {}", name);
    String configPath = args[1];
    String proxyConfig = args[2];
    Config config = ConfigFactory.load(ConfigFactory.parseFile(new File(configPath)));
    new ReverseProxyServer(proxyConfig).start(config);
    log.debug("proxy accepting connections");
  }
}
