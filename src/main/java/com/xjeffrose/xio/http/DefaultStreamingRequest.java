package com.xjeffrose.xio.http;

import com.google.auto.value.AutoValue;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.ToString;

/** Value class for representing a streaming outgoing HTTP1/2 Request, for use in a client. */
@UnstableApi
@AutoValue
@ToString
public abstract class DefaultStreamingRequest implements StreamingRequest {

  public abstract HttpMethod method();

  public abstract String path();

  public abstract Headers headers();

  /** Not intended to be called. */
  @Override
  public String version() {
    return "";
  }

  @Override
  public boolean keepAlive() {
    return false;
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder method(HttpMethod method);

    public abstract Builder path(String path);

    public abstract Builder headers(Headers headers);

    public abstract DefaultStreamingRequest build();
  }

  static Builder builder() {
    return new AutoValue_DefaultStreamingRequest.Builder();
  }
}
