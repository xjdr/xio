package com.xjeffrose.xio.client;

import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.UnpooledNode;
import io.netty.bootstrap.Bootstrap;
import java.net.InetSocketAddress;

public class SingleUnpooledNodeClient extends SingleNodeClient {

  public SingleUnpooledNodeClient(InetSocketAddress address, Bootstrap bootstrap) {
    super(address, bootstrap);
  }

  @Override
  public Node buildNode(InetSocketAddress address, Bootstrap bootstrap) {
    return new UnpooledNode(address, bootstrap);
  }
}
