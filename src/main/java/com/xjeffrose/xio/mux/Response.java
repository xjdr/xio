package com.xjeffrose.xio.mux;

import java.util.UUID;
import lombok.Value;

@Value
public class Response {
  private final UUID inResponseTo;
  private final Object payload;
}
