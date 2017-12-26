package com.xjeffrose.xio.tracing;

import brave.http.HttpClientAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import zipkin.Endpoint;

class XioHttpClientAdapter extends HttpClientAdapter<HttpRequest, HttpResponse> {

  private final boolean ssl;

  private StringBuilder newBuilder() {
    if (ssl) {
      return new StringBuilder("https://");
    } else {
      return new StringBuilder("http://");
    }
  }

  public XioHttpClientAdapter(boolean ssl) {
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
  public boolean parseServerAddress(HttpRequest request, Endpoint.Builder builder) {
    String host = request.headers().get(HttpHeaderNames.HOST);
    if (host != null) {
      String[] values = host.split(":");
      if (values.length > 1) {
        int port = Integer.parseInt(values[1]);
        builder.port(port);
      } else {
        int port;
        if (ssl) {
          port = 443;
        } else {
          port = 80;
        }
        builder.port(port);
      }
      if (values.length > 0) {
        return builder.parseIp(values[0]);
      }
    }
    return false;
  }
}
