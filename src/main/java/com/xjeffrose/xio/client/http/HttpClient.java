package com.xjeffrose.xio.client.http;

import brave.propagation.TraceContext;
import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpClient {

  private final XioClient client;
  private final String hostHeader;

  public HttpClient(XioClient client, String hostHeader) {
    this.client = client;
    this.hostHeader = hostHeader;
  }

  public void write(HttpRequest request, TraceContext context) {
    request.headers().set("Host", hostHeader);
    client.write(new XioRequest(request, context));
  }

  public void write(HttpRequest request) {
    write(request, null);
  }

  public void write(HttpContent content) {
    client.write(content);
  }

  public void write(LastHttpContent last) {
    client.write(last);
  }

}
