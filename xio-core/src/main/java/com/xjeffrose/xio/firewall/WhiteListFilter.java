package com.xjeffrose.xio.firewall;

import com.xjeffrose.xio.core.Constants;
import com.xjeffrose.xio.filter.IpFilterConfig;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class WhiteListFilter extends ChannelDuplexHandler {

  private final IpFilterConfig ipFilterConfig;

  public WhiteListFilter(IpFilterConfig ipFilterConfig) {
    this.ipFilterConfig = ipFilterConfig;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    InetAddress remoteAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();

    if (!ipFilterConfig.getWhitelist().contains(remoteAddress)) {
      ctx.channel().attr(Constants.IP_WHITE_LIST).set(Boolean.FALSE);
    }

    ctx.fireChannelActive();
  }
}
