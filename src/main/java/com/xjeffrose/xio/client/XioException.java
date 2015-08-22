package com.xjeffrose.xio.client;

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
