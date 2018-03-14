package com.xjeffrose.xio.http;

import static io.netty.util.AsciiString.CASE_INSENSITIVE_HASHER;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.CharSequenceValueConverter;

@UnstableApi
public class DefaultHeaders
    extends io.netty.handler.codec.DefaultHeaders<CharSequence, CharSequence, Headers>
    implements Headers {

  public DefaultHeaders() {
    super(
        CASE_INSENSITIVE_HASHER,
        CharSequenceValueConverter.INSTANCE,
        io.netty.handler.codec.DefaultHeaders.NameValidator.NOT_NULL);
  }
}
