package com.xjeffrose.xio.http;

import com.google.auto.value.AutoValue;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;
import lombok.ToString;

// TODO(CK): Consolidate Full/Streaming Response Builder into a single builder

/** Value class for representing an outgoing HTTP1/2 Response, for use in a server. */
@UnstableApi
@AutoValue
@ToString
public abstract class DefaultFullResponse implements FullResponse {

  @Override
  public boolean endOfMessage() {
    return true;
  }

  @Override
  public boolean startOfMessage() {
    return true;
  }

  public abstract ByteBuf body();

  public abstract HttpResponseStatus status();

  public abstract Headers headers();

  public abstract TraceInfo httpTraceInfo();

  @Override
  public int streamId() {
    return Message.H1_STREAM_ID_NONE;
  }

  /** Not intended to be called. */
  @Override
  public String version() {
    return "";
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder body(ByteBuf body);

    public abstract Builder status(HttpResponseStatus status);

    public abstract Builder headers(Headers headers);

    public abstract Builder httpTraceInfo(TraceInfo span);

    abstract Headers headers();

    public DefaultFullResponse build() {
      if (!httpTraceInfo().isPresent()) {
        httpTraceInfo(new TraceInfo(headers()));
      }
      return autoBuild();
    }

    abstract Optional<TraceInfo> httpTraceInfo();

    abstract DefaultFullResponse autoBuild();
  }

  public static Builder builder() {
    return new AutoValue_DefaultFullResponse.Builder();
  }
}
