package com.xjeffrose.xio.core;

public class XioIdleDisconnectException extends XioException {

  public XioIdleDisconnectException(String message) {
    super(message);
  }

  public XioIdleDisconnectException(String message, Throwable t) {
    super(message, t);
  }

  public XioIdleDisconnectException(Throwable t) {
    super(t);
  }
}
