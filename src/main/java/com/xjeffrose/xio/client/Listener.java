package com.xjeffrose.xio.client;

import org.jboss.netty.buffer.ChannelBuffer;

public interface Listener {

  void onRequestSent();

  void onResponseReceived(ChannelBuffer message);

  void onChannelError(XioException requestException);
}
