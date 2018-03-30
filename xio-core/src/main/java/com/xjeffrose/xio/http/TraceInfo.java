package com.xjeffrose.xio.http;

import brave.Span;
import java.util.Optional;

/** Mutable data object storing http tracing information. */
public class TraceInfo {

  private Span span = null;

  public Optional<Span> getSpan() {
    return Optional.ofNullable(span);
  }

  public void setSpan(Span span) {
    this.span = span;
  }
}
