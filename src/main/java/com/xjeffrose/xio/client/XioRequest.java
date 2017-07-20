package com.xjeffrose.xio.client;

import brave.propagation.TraceContext;
import lombok.Getter;

public class XioRequest<T> {

  @Getter
  private final T payload;
  @Getter
  private final TraceContext context;

  public XioRequest(T payload, TraceContext context) {
    this.payload = payload;
    this.context = context;
  }

  public boolean hasContext() {
    return context != null;
  }
}
