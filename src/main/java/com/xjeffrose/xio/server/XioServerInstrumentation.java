package com.xjeffrose.xio.server;

import java.net.InetSocketAddress;

public class XioServerInstrumentation {

  public InetSocketAddress addressBound;
  public String applicationProtocol;

  public XioServerInstrumentation() {
  }

  public String applicationProtocol() {
    return applicationProtocol;
  }

}
