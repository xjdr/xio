package com.xjeffrose.xio.client;

import com.xjeffrose.xio.client.loadbalancer.Node;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import java.io.Closeable;
import java.io.IOException;
import lombok.extern.log4j.Log4j;

@Log4j
abstract public class XioClient implements Closeable {

  protected final Bootstrap bootstrap;

  protected XioClient(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  abstract public Node getNode();

  public boolean write(ByteBuf msg) {
    return getNode().send(msg);
  }

  public boolean write(Object msg) {
    return getNode().send(msg);
  }
}
