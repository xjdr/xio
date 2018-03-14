package com.xjeffrose.xio.http;

import com.google.auto.value.AutoValue;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import lombok.ToString;

@UnstableApi
@AutoValue
@ToString
public abstract class DefaultStreamingData implements StreamingData {
  public abstract ByteBuf content();

  public abstract boolean endOfStream();

  public abstract Headers trailingHeaders();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder content(ByteBuf content);

    public abstract Builder endOfStream(boolean endOfStream);

    public abstract Builder trailingHeaders(Headers headers);

    public abstract DefaultStreamingData build();
  }

  public static Builder builder() {
    return new AutoValue_DefaultStreamingData.Builder().trailingHeaders(new DefaultHeaders());
  }
}
