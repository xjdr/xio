package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;

/** Interface representing a segmented HTTP1/2 request */
@UnstableApi
public interface SegmentedData {

  /**
   * See: <a href=https://tools.ietf.org/html/rfc7540#section-5>rfc</a>
   *
   * @return the stream id of the http2 connection stream or {@link Message#H1_STREAM_ID_NONE} if http1
   */
  int streamId();

  ByteBuf content();

  boolean endOfMessage();

  Headers trailingHeaders();
}
