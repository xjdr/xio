package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.Request;

public class MessageMetaState {
  public final Request request;
  public boolean requestFinished;
  public boolean responseFinished;

  public MessageMetaState(Request request, boolean requestFinished) {
    this.request = request;
    this.requestFinished = requestFinished;
    this.responseFinished = false;
  }
}
