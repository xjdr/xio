package com.xjeffrose.xio.client.loadbalancer;

public interface ECV {

  int getPort();

  String getPath();

  String getRequest();

  boolean veryifyResponse();

  boolean isOkToTakeTraffic();
}
