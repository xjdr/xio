package com.xjeffrose.xio.client.http;

import com.google.common.base.Preconditions;
import com.xjeffrose.xio.client.XioClientBootstrap;
import com.xjeffrose.xio.client.loadbalancer.Protocol;
import java.net.InetSocketAddress;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpClientBuilder {

  private final XioClientBootstrap bootstrap;
  private InetSocketAddress endpoint;
  private String hostHeader;
  private boolean useSsl = true;

  public HttpClientBuilder(XioClientBootstrap bootstrap) {
    this.bootstrap = bootstrap;
    bootstrap.proto(Protocol.HTTPS);
  }

  public HttpClientBuilder endpoint(InetSocketAddress endpoint) {
    this.endpoint = endpoint;
    hostHeader = endpoint.getHostString() + ":" + endpoint.getPort();
    return this;
  }

  public HttpClientBuilder endpointForUrl(URL url) {
    this.endpoint = Urls.getEndpoint(url);
    hostHeader = Urls.getHostHeader(url);
    useSsl = (url.getProtocol().equals("https"));
    return this;
  }

  public HttpClientBuilder endpointForUrl(String raw) {
    return endpointForUrl(Urls.parse(raw));
  }

  public HttpClientBuilder useSsl(boolean useSsl) {
    this.useSsl = useSsl;
    return this;
  }

  HttpClient build() {
    Preconditions.checkNotNull(endpoint, "endpoint must be defined");
    bootstrap.address(endpoint).ssl(useSsl);
    return new HttpClient(bootstrap.build(), hostHeader);
  }
}
