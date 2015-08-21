package com.xjeffrose.xio.client;

import org.jboss.netty.buffer.ChannelBuffer;

public interface RequestChannel {

  void sendAsynchronousRequest(final ChannelBuffer request,
                               final boolean oneway,
                               final Listener listener);


  void close();

  boolean hasError();

  public interface Listener {

    void onRequestSent();

    void onResponseReceived(ChannelBuffer message);

    void onChannelError(Exception requestException);
  }
}
