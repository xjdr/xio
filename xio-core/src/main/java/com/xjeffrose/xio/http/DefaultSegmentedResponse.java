package com.xjeffrose.xio.http;

import com.google.auto.value.AutoValue;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;
import lombok.ToString;

/** Value class for representing a segmented outgoing HTTP1/2 Response, for use in a server. */
@UnstableApi
@AutoValue
@ToString
public abstract class DefaultSegmentedResponse implements SegmentedResponse {

  @Override
  public abstract HttpResponseStatus status();

  @Override
  public abstract Headers headers();

  @Override
  public abstract TraceInfo httpTraceInfo();

  @Override
  public boolean startOfMessage() {
    return true;
  }

  @Override
  public boolean endOfMessage() {
    return false;
  }

  @Override
  public abstract int streamId();

  /** Not intended to be called. */
  @Override
  public String version() {
    return "";
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder status(HttpResponseStatus status);

    public abstract Builder headers(Headers headers);

    public abstract Builder httpTraceInfo(TraceInfo traceInfo);

    public abstract Builder streamId(int streamId);

    public DefaultSegmentedResponse build() {
      if (!httpTraceInfo().isPresent()) {
        httpTraceInfo(new TraceInfo(headers()));
      }
      return autoBuild();
    }

    abstract Headers headers();

    abstract Optional<TraceInfo> httpTraceInfo();

    abstract DefaultSegmentedResponse autoBuild();
  }

  public static Builder from(SegmentedResponse other) {
    return builder()
        .status(other.status())
        .headers(other.headers())
        .httpTraceInfo(other.httpTraceInfo())
        .streamId(other.streamId());
  }

  public static Builder builder() {
    return new AutoValue_DefaultSegmentedResponse.Builder().streamId(Message.H1_STREAM_ID_NONE);
  }
}
