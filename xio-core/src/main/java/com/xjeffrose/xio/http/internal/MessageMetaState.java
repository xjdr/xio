package com.xjeffrose.xio.http.internal;

import com.google.common.base.Preconditions;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Response;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MessageMetaState {
  @Nullable public final Request request;
  @Nullable public final Response response;
  public boolean requestFinished;
  public boolean responseFinished;

  public MessageMetaState(@Nonnull Response response, boolean responseFinished) {
    Preconditions.checkNotNull(response);
    this.request = null;
    this.response = response;
    this.requestFinished = true;
    this.responseFinished = responseFinished;
  }

  public MessageMetaState(@Nonnull Request request, boolean requestFinished) {
    Preconditions.checkNotNull(request);
    this.request = request;
    this.response = null;
    this.requestFinished = requestFinished;
    this.responseFinished = false;
  }
}
