package com.xjeffrose.xio.http;

import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2Request<T> {

  final int streamId;
  final T payload;

  public Http2Request(int streamId, T payload) {
    this.streamId = streamId;
    this.payload = payload;
  }

  public static Http2Request<Http2DataFrame> build(int streamId, Http2DataFrame data) {
    return new Http2Request<Http2DataFrame>(streamId, data);
  }

  public static Http2Request<Http2Headers> build(int streamId, Http2Headers headers) {
    return new Http2Request<Http2Headers>(streamId, headers);
  }
}
