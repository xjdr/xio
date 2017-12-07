package com.xjeffrose.xio.http;

import io.netty.handler.codec.http.HttpMethod;
import com.xjeffrose.xio.core.internal.UnstableApi;
import java.util.Map.Entry;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;

@UnstableApi
public abstract class Headers
  implements io.netty.handler.codec.Headers<CharSequence, CharSequence, Headers>,
             Iterable<Entry<CharSequence, CharSequence>> {


  public HttpHeaders http1Headers() {
    HttpHeaders result = new DefaultHttpHeaders();
    for (Entry<CharSequence, CharSequence> entry : this) {
      result.add(entry.getKey(), entry.getValue());
    }
    return result;
  }
}
