package com.xjeffrose.xio.client;

import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.Node;
import io.netty.bootstrap.Bootstrap;

import java.io.IOException;

public class MultiNodeClient extends XioClient {

  private final Distributor distributor;

  public MultiNodeClient(Distributor distributor, Bootstrap bootstrap) {
    super(bootstrap);
    this.distributor = distributor;
  }

  @Override
  public Node getNode() {
    return distributor.pick();
  }

  @Override
  public void close() throws IOException {
    distributor.close();
  }
}
