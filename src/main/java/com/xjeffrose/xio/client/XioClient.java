package com.xjeffrose.xio.client;

import com.xjeffrose.xio.client.loadbalancer.Node;
import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract public class XioClient implements Closeable {

  @Getter
  protected final Bootstrap bootstrap;

  protected XioClient(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  abstract public Node getNode();

  public Future<Void> write(Object msg) {
    return getNode().send(msg);
  }

  public Future<Void> write(HttpRequest request) {
    if (!request.headers().contains(HttpHeaderNames.HOST)) {
      SocketAddress address = bootstrap.config().remoteAddress();
      if (address instanceof InetSocketAddress) {
        InetSocketAddress socketAddress = (InetSocketAddress)address;
        String value = socketAddress.getHostString() + ":" + socketAddress.getPort();
        request.headers().set(HttpHeaderNames.HOST, value);
      }
    }

    return write((Object)request);
  }
}
