package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import java.util.Map.Entry;

@UnstableApi
public interface Headers
    extends io.netty.handler.codec.Headers<CharSequence, CharSequence, Headers>,
        Iterable<Entry<CharSequence, CharSequence>> {

  default HttpHeaders http1Headers() {
    HttpHeaders result = new DefaultHttpHeaders();
    for (Entry<CharSequence, CharSequence> entry : this) {
      result.add(entry.getKey(), entry.getValue());
    }
    return result;
  }
}
