package com.xjeffrose.xio.server;

import com.xjeffrose.xio.core.ZkClient;
import io.netty.channel.ChannelHandlerContext;
import java.util.HashSet;

public class XioBehavioralRuleEngine extends XioFirewall {
  private boolean noOp;

  public XioBehavioralRuleEngine(boolean noOp) {
    super(noOp);
    this.noOp = noOp;
  }

  public XioBehavioralRuleEngine(HashSet blacklist, HashSet whitelist) {
    super(blacklist, whitelist);
  }

  public XioBehavioralRuleEngine(ZkClient zkClient, boolean b) {
    super(zkClient, b);
  }

  @Override
  void runRuleSet(ChannelHandlerContext ctx, Object msg) {}
}
