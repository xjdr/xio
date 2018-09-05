package com.xjeffrose.xio.tracing;

import com.xjeffrose.xio.http.Request;
import io.opentracing.propagation.TextMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class HttpHeadersExtractAdapter implements TextMap {
  private final Request request;

  public HttpHeadersExtractAdapter(final Request request) {
    this.request = request;
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    Map<String, String> map =
        StreamSupport.stream(request.headers().spliterator(), true)
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey().toString(), entry -> entry.getValue().toString()));
    return map.entrySet().iterator();
  }

  @Override
  public void put(final String key, final String value) {
    throw new UnsupportedOperationException(
        "This class should be used only with Tracer.extract()!");
  }
}
