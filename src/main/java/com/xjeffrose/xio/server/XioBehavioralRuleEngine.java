package com.xjeffrose.xio.server;

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

}
