package com.xjeffrose.xio.http;

import brave.Span;
import javax.annotation.Nullable;

public interface Traceable {

  @Nullable
  Span traceSpan();
}
