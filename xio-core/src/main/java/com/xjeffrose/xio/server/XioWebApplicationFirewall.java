package com.xjeffrose.xio.server;

import com.xjeffrose.xio.core.ZkClient;
import io.netty.channel.ChannelHandlerContext;
import java.util.HashSet;

public class XioWebApplicationFirewall extends XioFirewall {

  public XioWebApplicationFirewall(boolean noOp) {
    super(noOp);
  }

  public XioWebApplicationFirewall(HashSet blacklist, HashSet whitelist) {
    super(blacklist, whitelist);
  }

  public XioWebApplicationFirewall(ZkClient zkClient, boolean b) {
    super(zkClient, b);
  }

  @Override
  void runRuleSet(ChannelHandlerContext ctx, Object msg) {}
}
