package com.xjeffrose.xio.client;

import io.netty.buffer.ByteBuf;

public interface Listener {

  void onRequestSent();

  void onResponseReceived(ByteBuf message);

  void onChannelError(XioException requestException);
}
