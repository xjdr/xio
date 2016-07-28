package com.xjeffrose.xio.marshall;

import com.xjeffrose.xio.config.HostnameDeterministicRuleEngineConfig;
import com.xjeffrose.xio.config.Http1DeterministicRuleEngineConfig;
import com.xjeffrose.xio.config.IpAddressDeterministicRuleEngineConfig;

public interface Unmarshaller {

  public void unmarshall(HostnameDeterministicRuleEngineConfig config, byte[] data);

  public void unmarshall(Http1DeterministicRuleEngineConfig config, byte[] data);

  public void unmarshall(IpAddressDeterministicRuleEngineConfig config, byte[] data);

}
