package com.xjeffrose.xio.clientBak;

import io.netty.buffer.ByteBuf;

public interface Listener {

  void onRequestSent();

  void onResponseReceived(ByteBuf message);

  void onChannelError(XioException requestException);
}
