package com.xjeffrose.xio.mux;

import java.util.UUID;
import lombok.Getter;

public class ServerRequest {

  @Getter private final UUID id;
  private final boolean expectsResponse;
  @Getter private final Object payload;

  public ServerRequest(UUID id, boolean expectsResponse, Object payload) {
    this.id = id;
    this.expectsResponse = expectsResponse;
    this.payload = payload;
  }

  public boolean expectsResponse() {
    return expectsResponse;
  }
}
