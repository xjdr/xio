package com.xjeffrose.xio;

import java.net.*;

class IpFilter extends Filter {

  IpFilter() {
  }

  public boolean filter(SocketAddress remoteAddress) {
    return false;
  }

}
