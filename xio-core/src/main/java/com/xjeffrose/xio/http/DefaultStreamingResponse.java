package com.xjeffrose.xio.http;

import com.google.auto.value.AutoValue;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.ToString;

/** Value class for representing a streaming outgoing HTTP1/2 Response, for use in a server. */
@UnstableApi
@AutoValue
@ToString
public abstract class DefaultStreamingResponse implements StreamingResponse {

  public abstract HttpResponseStatus status();

  public abstract Headers headers();
  /** Not intended to be called. */
  @Override
  public String version() {
    return "";
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder status(HttpResponseStatus status);

    public abstract Builder headers(Headers headers);

    public abstract DefaultStreamingResponse build();
  }

  public static Builder builder() {
    return new AutoValue_DefaultStreamingResponse.Builder();
  }
}
