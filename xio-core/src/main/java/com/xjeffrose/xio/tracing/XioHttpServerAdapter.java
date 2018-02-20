package com.xjeffrose.xio.tracing;

import brave.http.HttpServerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import zipkin.Endpoint;

class XioHttpServerAdapter extends HttpServerAdapter<HttpRequest, HttpResponse> {

  private final boolean ssl;

  private StringBuilder newBuilder() {
    if (ssl) {
      return new StringBuilder("https://");
    } else {
      return new StringBuilder("http://");
    }
  }

  public XioHttpServerAdapter(boolean ssl) {
    this.ssl = ssl;
  }

  @Override
  public String method(HttpRequest request) {
    return request.method().name();
  }

  @Override
  public String url(HttpRequest request) {
    StringBuilder url =
        newBuilder().append(request.headers().get(HttpHeaderNames.HOST)).append(request.uri());
    return url.toString();
  }

  @Override
  public String requestHeader(HttpRequest request, String name) {
    return request.headers().get(name);
  }

  @Override
  public Integer statusCode(HttpResponse response) {
    return response.status().code();
  }

  @Override
  public boolean parseClientAddress(HttpRequest request, Endpoint.Builder builder) {
    if (super.parseClientAddress(request, builder)) {
      return true;
    }
    String remoteIp = request.headers().get("x-remote-ip");
    return remoteIp != null && builder.parseIp(remoteIp);
  }
}
