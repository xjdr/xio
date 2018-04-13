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

  public abstract HttpResponseStatus status();

  public abstract Headers headers();

  public abstract TraceInfo httpTraceInfo();

  @Override
  public int streamId() {
    return Message.H1_STREAM_ID_NONE;
  }

  @Override
  public boolean startOfMessage() {
    return true;
  }

  @Override
  public boolean endOfMessage() {
    return false;
  }

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

  public static Builder builder() {
    return new AutoValue_DefaultSegmentedResponse.Builder();
  }
}
