package com.xjeffrose.xio.backend.server;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;


public class RestHandlers {

  private static final AsciiString X_TAG = AsciiString.of("x-tag");
  private static AsciiString X_TAG_VALUE = AsciiString.of("no tag");

  static void setTag(String tag) {
    X_TAG_VALUE = AsciiString.of(tag);
  }

  public ImmutableMap<String, RequestHandler> handlers() {
    return ImmutableMap.of(
      "/", responseBuilder -> responseBuilder.setBody(new PojoResponse("Release", "the Kraken"))
        .addHeader(X_TAG, X_TAG_VALUE)
        .addHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatus(HttpResponseStatus.OK));
  }
}
