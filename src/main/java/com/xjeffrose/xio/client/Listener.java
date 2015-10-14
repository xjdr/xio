package com.xjeffrose.xio.client;

import com.xjeffrose.xio.core.XioException;
import io.netty.util.ReferenceCounted;

public interface Listener<T extends ReferenceCounted> {

  void onRequestSent();

  void onResponseReceived(T message);

  void onChannelError(XioException requestException);

  T getResponse();
}
