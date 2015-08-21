package com.xjeffrose.xio.client;

public class XioClientException extends Throwable {
  public XioClientException(String s) {
    super(s);
  }

  public XioClientException(String s, Throwable t) {
    super(s, t);
  }
}
