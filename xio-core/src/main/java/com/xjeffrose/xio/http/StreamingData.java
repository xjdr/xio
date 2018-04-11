package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;

/** Interface representing a partial HTTP1/2 Request */
@UnstableApi
public interface StreamingData {

  ByteBuf content();

  boolean endOfMessage();

  Headers trailingHeaders();
}
