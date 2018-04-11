package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.EmptyHeaders;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.StreamingData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.ToString;

/** Wrap an incoming ByteBuf/Http2Headers, for use by streaming clients or servers */
@ToString
public class Http2StreamingData implements StreamingData {

  private final ByteBuf content;
  private final boolean eos;
  private final Headers trailingHeaders;

  public Http2StreamingData(ByteBuf content, boolean eos) {
    this.content = content;
    this.eos = eos;
    trailingHeaders = EmptyHeaders.INSTANCE;
  }

  public Http2StreamingData(Http2Headers headers) {
    this.content = Unpooled.EMPTY_BUFFER;
    eos = true;
    trailingHeaders = new Http2HeadersWrapper(headers);
  }

  public ByteBuf content() {
    return content;
  }

  public boolean endOfMessage() {
    return eos;
  }

  public Headers trailingHeaders() {
    return trailingHeaders;
  }
}
