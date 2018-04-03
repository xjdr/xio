package com.xjeffrose.xio.http;

import com.google.auto.value.AutoValue;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import java.util.Optional;
import lombok.ToString;

/** Value class for representing an outgoing HTTP1/2 Request, for use in a client. */
@UnstableApi
@AutoValue
@ToString
public abstract class DefaultFullRequest implements FullRequest {

  @Override
  public boolean startOfStream() {
    return true;
  }

  public abstract ByteBuf body();

  public abstract HttpMethod method();

  public abstract String path();

  public abstract Headers headers();

  public abstract int streamId();

  public abstract TraceInfo httpTraceInfo();

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
    public abstract Builder body(ByteBuf body);

    public abstract Builder method(HttpMethod method);

    public abstract Builder path(String path);

    public abstract Builder headers(Headers headers);

    public abstract Builder streamId(int streamId);

    public abstract Builder httpTraceInfo(TraceInfo traceInfo);

    abstract Optional<Headers> headers();

    public Builder host(String host) {
      if (!headers().isPresent()) {
        headers(new DefaultHeaders());
      }

      headers().get().set(HttpHeaderNames.HOST, host);
      return this;
    }

    abstract Optional<TraceInfo> httpTraceInfo();

    abstract DefaultFullRequest autoBuild();

    public DefaultFullRequest build() {
      if (!httpTraceInfo().isPresent() && headers().isPresent()) {
        httpTraceInfo(new TraceInfo(headers().get()));
      }
      return autoBuild();
    }
  }

  public static Builder builder() {
    return new AutoValue_DefaultFullRequest.Builder().streamId(-1);
  }
}
