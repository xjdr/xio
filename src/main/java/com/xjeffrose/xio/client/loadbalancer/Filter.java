package com.xjeffrose.xio.client.loadbalancer;


public interface Filter {
  boolean contains(String ServiceName, String hostname, String env);
}
