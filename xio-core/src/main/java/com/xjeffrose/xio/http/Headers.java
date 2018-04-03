package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@UnstableApi
public interface Headers
    extends io.netty.handler.codec.Headers<CharSequence, CharSequence, Headers>,
        Iterable<Entry<CharSequence, CharSequence>> {

  @Nullable
  CharSequence get(CharSequence name);

  @Nullable
  default String get(String name) {
    CharSequence value = get((CharSequence) name);
    if (value != null) {
      return value.toString();
    } else {
      return null;
    }
  }

  /**
   * @param isTrailer this Headers object will be used for trailers.
   * @param isRequest this Headers object will be used in a request header.
   * @return an Http1 Headers object based on the values in this Headers object.
   */
  default HttpHeaders http1Headers(boolean isTrailer, boolean isRequest) {
    // TODO(CK): filter out headers that can't be in a trailer
    // TODO(CK): filter out headers that can't be in a request
    HttpHeaders result = new DefaultHttpHeaders();
    for (Entry<CharSequence, CharSequence> entry : this) {
      result.add(entry.getKey(), entry.getValue());
    }
    return result;
  }

  default Http2Headers http2Headers() {
    Http2Headers result = new DefaultHttp2Headers();
    for (Entry<CharSequence, CharSequence> entry : this) {
      result.add(entry.getKey(), entry.getValue());
    }
    return result;
  }
}
