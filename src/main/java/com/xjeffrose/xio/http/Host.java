package com.xjeffrose.xio.http;

import java.net.InetSocketAddress;
import lombok.Value;

@Value
public class Host {

  private InetSocketAddress address;
  private String hostHeader;
  private boolean needSSL;
}
