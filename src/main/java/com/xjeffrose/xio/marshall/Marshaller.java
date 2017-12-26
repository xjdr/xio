package com.xjeffrose.xio.marshall;

import com.xjeffrose.xio.config.HostnameDeterministicRuleEngineConfig;
import com.xjeffrose.xio.config.Http1DeterministicRuleEngineConfig;
import com.xjeffrose.xio.config.IpAddressDeterministicRuleEngineConfig;

public interface Marshaller {

  public byte[] marshall(HostnameDeterministicRuleEngineConfig config);

  public byte[] marshall(Http1DeterministicRuleEngineConfig config);

  public byte[] marshall(IpAddressDeterministicRuleEngineConfig config);
}
