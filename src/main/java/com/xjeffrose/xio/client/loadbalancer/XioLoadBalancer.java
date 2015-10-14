package com.xjeffrose.xio.client.loadbalancer;

import java.net.InetSocketAddress;

public interface XioLoadBalancer {

  InetSocketAddress getNext();
}
