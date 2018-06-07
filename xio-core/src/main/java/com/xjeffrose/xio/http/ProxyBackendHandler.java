package com.xjeffrose.xio.http;

import com.xjeffrose.xio.http.internal.ProxyClientState;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyBackendHandler extends ChannelInboundHandlerAdapter {

  private final ChannelHandlerContext frontend;
  private boolean needFlush = false;

  private ChannelFutureListener errorListenter =
      (f) -> {
        if (f.cause() != null) {
          // TODO(CK): move this into a logger class
          log.error("Write Error!", f.cause());
        }
      };

  public ProxyBackendHandler(ChannelHandlerContext frontend) {
    this.frontend = frontend;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    log.debug("RawBackendHandler[{}] channelRead: {}", this, msg);
    if (msg instanceof Response) {
      Response response = (Response) msg;
      if (response.endOfMessage()) {
        frontend.writeAndFlush(msg).addListener(errorListenter);
      } else {
        frontend.write(msg).addListener(errorListenter);
      }
    } else {
      frontend.write(msg).addListener(errorListenter);
      needFlush = true;
    }
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
    frontend.fireUserEventTriggered(ProxyClientState.CLOSED);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.debug("RawBackendHandler[{}] exceptionCaught: {}", this, cause);
    ctx.close();
  }
}
