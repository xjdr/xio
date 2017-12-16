package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Builder(builderClassName = "Builder")
@Accessors(fluent = true)
@Getter
@UnstableApi
public class DefaultStreamingData implements StreamingData {
  ByteBuf content;
  boolean endOfStream;
  Headers trailingHeaders;
}
