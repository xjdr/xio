package com.xjeffrose.xio.client.chicago;

public enum Op {
  READ,
  WRITE,
  DELETE,
  RESPONSE,
  TS_WRITE,
  STREAM;

  public static Op fromValue(int ordinal) {
    return Op.values()[ordinal];
  }
}
