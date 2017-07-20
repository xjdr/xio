package com.xjeffrose.xio.server;

import java.net.InetSocketAddress;

public class XioServerInstrumentation {

  public InetSocketAddress addressBound;
  public String applicationProtocol;

  public XioServerInstrumentation() {
  }

  @Deprecated
  public InetSocketAddress addressBound() {
    return addressBound;
  }

  public InetSocketAddress boundAddress() {
    return addressBound;
  }

  public String applicationProtocol() {
    return applicationProtocol;
  }

}
