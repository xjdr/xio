package com.xjeffrose.xio.http;

import com.google.auto.value.AutoValue;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import lombok.ToString;

@UnstableApi
@AutoValue
@ToString
public abstract class DefaultSegmentedData implements SegmentedData {
  public abstract ByteBuf content();

  public abstract boolean endOfMessage();

  public abstract Headers trailingHeaders();

  public abstract int streamId();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder content(ByteBuf content);

    public abstract Builder endOfMessage(boolean isEnd);

    public abstract Builder trailingHeaders(Headers headers);

    public abstract Builder streamId(int id);

    public abstract DefaultSegmentedData build();
  }

  public static Builder builder() {
    return new AutoValue_DefaultSegmentedData.Builder().trailingHeaders(new DefaultHeaders());
  }
}
