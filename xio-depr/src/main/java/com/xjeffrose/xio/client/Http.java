package com.xjeffrose.xio.client;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

public class Http {

  public static HttpRequest get(String host, String path) {
    HttpHeaders headers = new DefaultHttpHeaders();
    headers.set(HttpHeaderNames.HOST, host);
    headers.set(HttpHeaderNames.CONTENT_LENGTH, 0);
    return new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path, headers);
  }

  public static HttpRequest get(String path) {
    return new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
  }

  public static HttpRequest post(String host, String path, String content) {
    HttpHeaders headers = new DefaultHttpHeaders();
    headers.set(HttpHeaderNames.HOST, host);
    headers.set(HttpHeaderNames.CONTENT_LENGTH, content.length());
    return new DefaultFullHttpRequest(
        HttpVersion.HTTP_1_1,
        HttpMethod.POST,
        path,
        Unpooled.wrappedBuffer(content.getBytes()),
        headers,
        new DefaultHttpHeaders());
  }
}
