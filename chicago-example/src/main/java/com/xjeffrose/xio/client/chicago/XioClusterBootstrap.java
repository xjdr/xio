package com.xjeffrose.xio.client.chicago;

import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoopFactory;
import io.netty.bootstrap.Bootstrap;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class XioClusterBootstrap {
  public class Config {
    final Bootstrap bootstrap;
    final List<InetSocketAddress> nodeAddresses;
    int quorum;
    AsyncRetryLoopFactory retryLoopFactory;

    Config(Bootstrap bootstrap) {
      this.bootstrap = bootstrap;
      nodeAddresses = new ArrayList<InetSocketAddress>();
    }

    public Bootstrap bootstrap() {
      return bootstrap;
    }

    public List<InetSocketAddress> nodes() {
      return nodeAddresses;
    }

    public int quorum() {
      return quorum;
    }

    public AsyncRetryLoopFactory retryLoopFactory() {
      return retryLoopFactory;
    }
  }

  private final Config config;

  XioClusterBootstrap(Bootstrap bootstrap) {
    config = new Config(bootstrap);
  }

  public XioClusterBootstrap addNode(InetSocketAddress address) {
    config.nodeAddresses.add(address);
    return this;
  }

  public XioClusterBootstrap addNodes(List<InetSocketAddress> addresses) {
    config.nodeAddresses.addAll(addresses);
    return this;
  }

  public XioClusterBootstrap quorum(int quorum) {
    config.quorum = quorum;
    return this;
  }

  public XioClusterBootstrap retryLoopFactory(AsyncRetryLoopFactory retryLoopFactory) {
    config.retryLoopFactory = retryLoopFactory;
    return this;
  }

  public Config config() {
    return config;
  }
}
