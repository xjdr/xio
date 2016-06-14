package com.xjeffrose.xio.server;

import java.util.HashSet;

public class XioWebApplicationFirewall extends XioFirewall {

  public XioWebApplicationFirewall(boolean noOp) {
    super(noOp);
  }

  public XioWebApplicationFirewall(HashSet blacklist, HashSet whitelist) {
    super(blacklist, whitelist);
  }
}
