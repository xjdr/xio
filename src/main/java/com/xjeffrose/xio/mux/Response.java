package com.xjeffrose.xio.mux;

import lombok.Value;

import java.util.UUID;

@Value
public class Response {
  private final UUID inResponseTo;
  private final Object payload;
}
