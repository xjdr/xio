package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;

/** Interface representing a segmented HTTP1/2 request */
@UnstableApi
public interface SegmentedData {

  ByteBuf content();

  boolean endOfMessage();

  Headers trailingHeaders();
}
