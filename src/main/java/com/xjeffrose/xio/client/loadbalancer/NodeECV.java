package com.xjeffrose.xio.client.loadbalancer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.log4j.Logger;

public class NodeECV extends ChannelInboundHandlerAdapter {
  private static final Logger log = Logger.getLogger(NodeECV.class.getName());

  private final Node node;
  private final Protocol proto;
  private final ECV ecv;

  public NodeECV(Node node, Protocol proto, ECV ecv) {
    this.node = node;
    this.proto = proto;
    this.ecv = ecv;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest) msg;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("Exception in ECV check for node " + node.address() + ": ", cause);
    node.setAvailable(false);
    ctx.close();
  }

}
