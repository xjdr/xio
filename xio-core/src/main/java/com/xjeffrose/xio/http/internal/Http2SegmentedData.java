package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.EmptyHeaders;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.SegmentedData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.ToString;

/** Wrap an incoming ByteBuf/Http2Headers, for use by streaming clients or servers */
@ToString
public class Http2SegmentedData implements SegmentedData {

  private final ByteBuf content;
  private final boolean eos;
  private final Headers trailingHeaders;
  private final int streamId;

  public Http2SegmentedData(ByteBuf content, boolean eos, int streamId) {
    this.content = content;
    this.eos = eos;
    this.streamId = streamId;
    trailingHeaders = EmptyHeaders.INSTANCE;
  }

  public Http2SegmentedData(Http2Headers headers, int streamId) {
    this.streamId = streamId;
    this.content = Unpooled.EMPTY_BUFFER;
    eos = true;
    trailingHeaders = new Http2HeadersWrapper(headers);
  }

  @Override
  public int streamId() {
    return streamId;
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
