package com.xjeffrose.xio.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;

public class XioIdleDisconnectHandler extends IdleStateHandler{
  public XioIdleDisconnectHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
    super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
  }

  public XioIdleDisconnectHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
    super(readerIdleTime, writerIdleTime, allIdleTime, unit);
  }

  @Override
  protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
    ctx.channel().close();

    throw new XioIdleDisconnectException("Disconnecting " + ctx.channel() + " due to idle state");
  }
}
