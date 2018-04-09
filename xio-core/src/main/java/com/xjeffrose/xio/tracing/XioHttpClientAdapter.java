package com.xjeffrose.xio.tracing;

import brave.http.HttpClientAdapter;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Response;
import io.netty.handler.codec.http.HttpHeaderNames;
import javax.annotation.ParametersAreNonnullByDefault;
import zipkin.Endpoint;

@ParametersAreNonnullByDefault
class XioHttpClientAdapter extends HttpClientAdapter<Request, Response> {

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
  public String method(Request request) {
    return request.method().name();
  }

  @Override
  public String url(Request request) {
    StringBuilder url =
        newBuilder().append(request.headers().get(HttpHeaderNames.HOST)).append(request.path());
    return url.toString();
  }

  @Override
  public String requestHeader(Request request, String name) {
    return request.headers().get(name);
  }

  @Override
  public Integer statusCode(Response response) {
    return response.status().code();
  }

  @Override
  public boolean parseServerAddress(Request request, Endpoint.Builder builder) {
    String host = request.headers().get(HttpHeaderNames.HOST.toString());
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
