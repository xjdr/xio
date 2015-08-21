package com.xjeffrose.xio.client;

import java.net.URI;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class XioHttpRequestFactory {

  public XioHttpRequestFactory() {

  }

  public static HttpRequest getDefaultRequest(URI uri) {
    // Get the goods
    String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
    String host = uri.getHost() == null ? "localhost" : uri.getHost();
    int port = uri.getPort();
    if (port == -1) {
      if ("http".equalsIgnoreCase(scheme)) {
        port = 80;
      } else if ("https".equalsIgnoreCase(scheme)) {
        port = 443;
      }
    }

    // Prepare the HTTP request.
    HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());

    request.setHeader(HttpHeaders.Names.HOST, host);
    request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, 0);
    request.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/text");
    request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
    request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
    request.setHeader(HttpHeaders.Names.USER_AGENT, "xio");

    // Set some example cookies.
    CookieEncoder httpCookieEncoder = new CookieEncoder(false);
    httpCookieEncoder.addCookie("my-cookie", "foo");
    httpCookieEncoder.addCookie("another-cookie", "bar");
    request.setHeader(HttpHeaders.Names.COOKIE, httpCookieEncoder.encode());

    return request;
  }
}
