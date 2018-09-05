package com.xjeffrose.xio.tracing;

import com.xjeffrose.xio.http.Request;
import io.opentracing.propagation.TextMap;
import java.util.Iterator;
import java.util.Map;

public class HttpHeadersInjectAdapter implements TextMap {
  private final Request httpRequest;

  public HttpHeadersInjectAdapter(final Request httpRequest) {
    this.httpRequest = httpRequest;
  }

  @Override
  public void put(final String key, final String value) {
    httpRequest.headers().set(key.toLowerCase(), value);
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    throw new UnsupportedOperationException("This class should be used only with tracer#inject()");
  }
}
