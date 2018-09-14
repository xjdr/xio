package com.xjeffrose.xio.zookeeper;

import lombok.Value;

@Value
public class DeploymentPayload {
  String host;
  int port;
}
