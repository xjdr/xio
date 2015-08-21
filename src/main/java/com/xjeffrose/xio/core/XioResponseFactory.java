package com.xjeffrose.xio.core;

import java.util.Map;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class XioResponseFactory {

  public static HttpMessage getResponse(Map<Integer, HttpMessage> responseMap) {

    HttpMessage httpResponse = (HttpMessage) new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

    return httpResponse;
  }
}
