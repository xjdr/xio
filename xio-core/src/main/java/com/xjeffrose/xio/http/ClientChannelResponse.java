package com.xjeffrose.xio.http;

import javax.annotation.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

public class ClientChannelResponse {
  @Accessors(fluent = true)
  @Getter
  private boolean isDone;

  @Getter private boolean isSuccess;
  @Getter private Throwable cause;

  public ClientChannelResponse(boolean isDone, boolean isSuccess, @Nullable Throwable cause) {
    this.isDone = isDone;
    this.isSuccess = isSuccess;
    this.cause = cause;
  }
}
