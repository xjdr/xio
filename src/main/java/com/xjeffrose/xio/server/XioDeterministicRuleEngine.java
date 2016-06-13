package com.xjeffrose.xio.server;

import java.util.HashSet;

public class XioDeterministicRuleEngine extends XioFirewall {

  public XioDeterministicRuleEngine(boolean noOp) {
    super(noOp);
  }

  public XioDeterministicRuleEngine(HashSet blacklist) {
    super(blacklist);
  }
}
