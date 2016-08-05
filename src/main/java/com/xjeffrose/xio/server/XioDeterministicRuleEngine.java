package com.xjeffrose.xio.server;

import com.xjeffrose.xio.core.ZkClient;
import io.netty.channel.ChannelHandlerContext;
import java.util.HashSet;

/**
 * Allow or Deny traffic based on the incoming IP Address
 */
public class XioDeterministicRuleEngine extends XioFirewall {

  public XioDeterministicRuleEngine(boolean noOp) {
    super(noOp);
  }

  public XioDeterministicRuleEngine(HashSet blacklist, HashSet whitelist) {
    super(blacklist, whitelist);
  }

  public XioDeterministicRuleEngine(ZkClient zkClient, boolean b) {
    super(zkClient, b);
  }

  @Override
  void runRuleSet(ChannelHandlerContext ctx, Object msg) {

  }
}
