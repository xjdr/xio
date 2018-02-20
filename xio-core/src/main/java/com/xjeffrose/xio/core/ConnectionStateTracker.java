package com.xjeffrose.xio.core;

import io.netty.channel.Channel;
import java.util.UUID;

public interface ConnectionStateTracker {

  void setOutboundChannel(Channel outboundChannel);

  String toString(String prepend);

  String toString(String prepend, String postpend);

  void incrementWriteCount(Channel channel);

  void incrementReadCount(Channel channel);

  int getReadCount(Channel channel);

  UUID getId();

  Channel getInboundChannel();

  Channel getOutboundChannel();
}
