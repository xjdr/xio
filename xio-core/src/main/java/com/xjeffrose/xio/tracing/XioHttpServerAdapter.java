package com.xjeffrose.xio.tracing;

import brave.http.HttpServerAdapter;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Response;
import io.netty.handler.codec.http.HttpHeaderNames;
import javax.annotation.ParametersAreNonnullByDefault;
import zipkin.Endpoint;

@ParametersAreNonnullByDefault
class XioHttpServerAdapter extends HttpServerAdapter<Request, Response> {

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
  public boolean parseClientAddress(Request request, Endpoint.Builder builder) {
    if (super.parseClientAddress(request, builder)) {
      return true;
    }
    String remoteIp = request.headers().get("x-remote-ip");
    return remoteIp != null && builder.parseIp(remoteIp);
  }
}
