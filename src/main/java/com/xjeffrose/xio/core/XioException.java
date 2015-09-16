package com.xjeffrose.xio.core;

public class XioException extends Exception {
  public XioException(String message) {
    super(message);
  }

  public XioException(String message, Throwable t) {
    super(message, t);
  }

  public XioException(Throwable t) {
    super(t);
  }
}
