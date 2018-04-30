package com.xjeffrose.xio.http;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyBackendHandler extends ChannelInboundHandlerAdapter {

  private final ChannelHandlerContext frontend;
  private boolean needFlush = false;

  public ProxyBackendHandler(ChannelHandlerContext frontend) {
    this.frontend = frontend;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    log.debug("RawBackendHandler[{}] channelRead: {}", this, msg);
    frontend
        .write(msg)
        .addListener(
            (ChannelFutureListener)
                f -> {
                  if (f.cause() != null) {
                    // TODO(CK): move this into a logger class
                    log.error("Write Error!", f.cause());
                  }
                });
    needFlush = true;
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    log.debug("RawBackendHandler[{}] channelReadComplete", this);
    if (needFlush) {
      frontend.flush();
      needFlush = false;
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    log.debug("RawBackendHandler[{}] channelInactive", this);
    // TODO(CK): this should really be some sort of notification to the frontend
    // that the backend closed. Keepalive/h2 will require the connection to stay open, we
    // shouldn't be closing it.
    frontend.close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.debug("RawBackendHandler[{}] exceptionCaught: {}", this, cause);
    ctx.close();
  }
}
