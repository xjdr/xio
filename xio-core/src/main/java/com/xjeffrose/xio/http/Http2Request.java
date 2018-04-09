package com.xjeffrose.xio.http;

import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2Request<T> {

  final int streamId;
  final T payload;
  final boolean eos;

  public Http2Request(int streamId, T payload, boolean eos) {
    this.streamId = streamId;
    this.payload = payload;
    this.eos = eos;
  }

  public static Http2Request<Http2DataFrame> build(int streamId, Http2DataFrame data, boolean eos) {
    return new Http2Request<>(streamId, data, eos);
  }

  public static Http2Request<Http2Headers> build(int streamId, Http2Headers headers, boolean eos) {
    return new Http2Request<>(streamId, headers, eos);
  }
}
