package com.xjeffrose.xio.client;

import com.xjeffrose.xio.client.loadbalancer.Node;
import io.netty.bootstrap.Bootstrap;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SingleNodeClient extends XioClient {

  private final InetSocketAddress address;
  private final Node node;

  public SingleNodeClient(InetSocketAddress address, Bootstrap bootstrap) {
    super(bootstrap);
    this.address = address;
    this.node = buildNode();
  }

  public Node buildNode() {
    return new Node(address, bootstrap);
  }

  @Override
  public Node getNode() {
    return node;
  }

  @Override
  public void close() throws IOException {
    node.close();
  }
}
