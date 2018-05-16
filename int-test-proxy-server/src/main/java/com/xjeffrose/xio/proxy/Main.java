package com.xjeffrose.xio.proxy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
  public static void main(String[] args) throws Exception {
    //log.info("starting int-test-proxy-server");
    new ReverseProxyServer(true).start();
  }
}
