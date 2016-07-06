package com.xjeffrose.xio.server;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ZkClient;

public class XioServerState {

  private final ZkClient zkClient;
  private final ChannelStatistics channelStatistics;

  public XioServerState(ZkClient zkClient, ChannelStatistics channelStatistics) {
    this.zkClient = zkClient;
    this.channelStatistics = channelStatistics;
  }

  public ZkClient zkClient() {
    return zkClient;
  }

  public ChannelStatistics channelStatistics() {
    return channelStatistics;
  }

}
