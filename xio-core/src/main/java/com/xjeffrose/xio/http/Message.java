package com.xjeffrose.xio.http;

import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;

/**
 * Interface representing a HTTP1/2 Message (Request/Response) Message: A complete sequence of
 * frames that map to a logical request or response message. See: <a href= ></a>
 */
public interface Message {

  int H1_STREAM_ID_NONE = -1;

  boolean startOfMessage();

  boolean endOfMessage();

  String version();

  Headers headers();

  /**
   * See: <a href=https://tools.ietf.org/html/rfc7540#section-5>rfc</a>
   *
   * @return the stream id of the http2 connection stream or {@link #H1_STREAM_ID_NONE} if http1
   */
  int streamId();

  boolean hasBody();

  @Nullable
  ByteBuf body();

  TraceInfo httpTraceInfo();
}
