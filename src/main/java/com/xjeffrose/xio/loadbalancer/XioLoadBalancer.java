package com.xjeffrose.xio.loadbalancer;

import java.net.InetSocketAddress;

public interface XioLoadBalancer {

  InetSocketAddress getNext();
}
