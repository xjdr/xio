package com.xjeffrose.xio.core;

public class XioTransportException extends XioException {

  public XioTransportException(String message) {
    super(message);
  }

  public XioTransportException(String message, Throwable t) {
    super(message, t);
  }

  public XioTransportException(Throwable t) {
    super(t);
  }
}
