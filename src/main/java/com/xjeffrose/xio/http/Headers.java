package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import java.util.Map.Entry;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

@UnstableApi
public interface Headers
    extends io.netty.handler.codec.Headers<CharSequence, CharSequence, Headers>,
        Iterable<Entry<CharSequence, CharSequence>> {

  default HttpHeaders http1Headers(boolean isTrailer, boolean isRequest) {
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
