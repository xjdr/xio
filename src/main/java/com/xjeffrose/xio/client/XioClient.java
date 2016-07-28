package com.xjeffrose.xio.client;

import com.xjeffrose.xio.client.loadbalancer.Node;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Future;
import lombok.extern.log4j.Log4j;

import java.io.Closeable;
import java.io.IOException;

@Log4j
abstract public class XioClient implements Closeable {

  protected final Bootstrap bootstrap;

  protected XioClient(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  abstract public Node getNode();

  public Future<Void> write(Object msg) {
    return getNode().send(msg);
  }
}
