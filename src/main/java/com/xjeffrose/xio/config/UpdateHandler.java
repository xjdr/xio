package com.xjeffrose.xio.config;

import com.xjeffrose.xio.config.thrift.RuleType;
import java.net.InetAddress;

interface UpdateHandler {

  long commit();

  void process(UpdateType updateType, InetAddress address, RuleType ruleType);

}
