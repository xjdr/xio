package com.xjeffrose.xio.client;

import io.airlift.units.Duration;
import org.jboss.netty.channel.Channel;

public interface XioClientChannel extends RequestChannel {

  Duration getSendTimeout();

  void setSendTimeoutDuration(Duration sendTimeout);

  Duration getReceiveTimeout();

  void setReceiveTimeout(Duration receiveTimeout);

  Duration getReadTimeout();

  void setReadTimeout(Duration readTimeout);

  void executeInIoThread(Runnable runnable);

  Channel getNettyChannel();
}