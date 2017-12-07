package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@UnstableApi
public abstract class StreamingData {

  public abstract ByteBuf content();

  public abstract boolean endOfStream();

  public abstract Headers trailingHeaders();

  @Builder(builderClassName = "Builder")
  @Accessors(fluent = true)
  @Getter
  public static class StreamingDataImpl {
    ByteBuf content;
    boolean endOfStream;
    Headers trailingHeaders;
  }

}
