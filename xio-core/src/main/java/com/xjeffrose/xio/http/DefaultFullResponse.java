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

  @Override
  public abstract ByteBuf body();

  @Override
  public abstract HttpResponseStatus status();

  @Override
  public abstract Headers headers();

  @Override
  public abstract TraceInfo httpTraceInfo();

  @Override
  public abstract int streamId();

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

    public abstract Builder streamId(int streamId);

    public DefaultFullResponse build() {
      if (!httpTraceInfo().isPresent()) {
        httpTraceInfo(new TraceInfo(headers()));
      }
      return autoBuild();
    }

    abstract Headers headers();

    abstract Optional<TraceInfo> httpTraceInfo();

    abstract DefaultFullResponse autoBuild();
  }

  public static Builder from(FullResponse other) {
    return builder()
        .body(other.body())
        .headers(other.headers())
        .httpTraceInfo(other.httpTraceInfo())
        .streamId(other.streamId())
        .status(other.status())
        .body(other.body());
  }

  public static Builder builder() {
    return new AutoValue_DefaultFullResponse.Builder().streamId(Message.H1_STREAM_ID_NONE);
  }
}
