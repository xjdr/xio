package com.xjeffrose.xio.http;

import io.netty.handler.codec.http.HttpMethod;
import com.xjeffrose.xio.core.internal.UnstableApi;
import java.util.Map.Entry;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.CharSequenceValueConverter;

import static io.netty.util.AsciiString.CASE_INSENSITIVE_HASHER;

@UnstableApi
public class DefaultHeaders extends io.netty.handler.codec.DefaultHeaders<CharSequence, CharSequence, Headers> implements Headers {

  DefaultHeaders() {
    super(CASE_INSENSITIVE_HASHER, CharSequenceValueConverter.INSTANCE, io.netty.handler.codec.DefaultHeaders.NameValidator.NOT_NULL);
  }

}
