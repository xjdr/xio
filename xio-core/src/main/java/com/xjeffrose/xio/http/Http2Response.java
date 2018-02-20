package com.xjeffrose.xio.http;

import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
public class Http2Response<T> {

  final int streamId;
  final T payload;
  final boolean eos;

  public Http2Response(int streamId, T payload, boolean eos) {
    this.streamId = streamId;
    this.payload = payload;
    this.eos = eos;
  }

  public static Http2Response<Http2DataFrame> build(
      int streamId, Http2DataFrame data, boolean eos) {
    return new Http2Response<Http2DataFrame>(streamId, data, eos);
  }

  public static Http2Response<Http2Headers> build(int streamId, Http2Headers headers) {
    return new Http2Response<Http2Headers>(streamId, headers, false);
  }

  public static Http2Response<Http2Headers> build(int streamId, Http2Headers headers, boolean eos) {
    return new Http2Response<Http2Headers>(streamId, headers, eos);
  }

  public Http2Response newStreamId(int newId) {
    return new Http2Response(newId, payload, eos);
  }
}
