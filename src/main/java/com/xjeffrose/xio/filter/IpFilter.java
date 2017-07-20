package com.xjeffrose.xio.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;

public class IpFilter extends ChannelInboundHandlerAdapter {

  private final IpFilterConfig config;

  public IpFilter(IpFilterConfig config) {
    this.config = config;
  }

  /**
   * channel has been registered with it's eventloop, we may have a
   * remote ip, try to filter.
   */
  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    eagerFilter(ctx);
    ctx.fireChannelRegistered();
  }

  /**
   * channel is active, check remote ip against the filter.
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    filter(ctx, remoteAddress(ctx));
    ctx.fireChannelActive();
  }

  private InetSocketAddress remoteAddress(ChannelHandlerContext ctx) {
    @SuppressWarnings("unchecked")
    InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
    return address;
  }

  private void eagerFilter(ChannelHandlerContext ctx) throws Exception {
    InetSocketAddress address = remoteAddress(ctx);
    if (address != null) {
      filter(ctx, address);
    }
  }

  /**
   * If there is no remote ip or the ip is denied close the
   * ctx. Otherwise allow the connection to live. In either case
   * remove this handler from the pipeline.
   */
  private void filter(ChannelHandlerContext ctx, InetSocketAddress address) {
    ctx.pipeline().remove(this);
    if (address == null || config.denied(address)) {
      ctx.close();
    }
  }

}
