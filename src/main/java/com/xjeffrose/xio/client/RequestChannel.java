package com.xjeffrose.xio.client;

import com.xjeffrose.xio.core.XioException;
import io.netty.buffer.ByteBuf;
import java.io.Closeable;

public interface RequestChannel extends Closeable {

  void sendAsynchronousRequest(final ByteBuf request,
                               final boolean oneway,
                               final Listener listener) throws XioException;

  void close();

  boolean hasError();

  XioException getError();

  XioProtocolFactory getProtocolFactory();
}