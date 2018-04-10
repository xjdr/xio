package com.xjeffrose.xio.tracing;

import brave.Tracing;
import brave.context.slf4j.MDCCurrentTraceContext;
import brave.http.HttpTracing;
import brave.sampler.Sampler;
import com.typesafe.config.Config;
import lombok.NonNull;
import lombok.val;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.Reporter;
import zipkin.reporter.okhttp3.OkHttpSender;

public class XioTracing {

  private final Tracing tracing;

  Reporter<Span> buildReporter(@NonNull String zipkinUrl) {
    return AsyncReporter.builder(OkHttpSender.create(zipkinUrl)).build();
  }

  Tracing buildTracing(@NonNull String name, @NonNull String zipkinUrl, float samplingRate) {
    if (zipkinUrl.isEmpty() || !(samplingRate > 0f)) {
      return null;
    }
    return Tracing.newBuilder()
        .localServiceName(name)
        // puts trace IDs into logs
        .currentTraceContext(MDCCurrentTraceContext.create())
        .reporter(buildReporter(zipkinUrl))
        .sampler(Sampler.create(samplingRate))
        .build();
  }

  public XioTracing(Config config) {
    val name = config.getString("name");
    val zipkinUrl = config.getString("settings.tracing.zipkinUrl");
    float samplingRate = ((Double) config.getDouble("settings.tracing.samplingRate")).floatValue();
    tracing = buildTracing(name, zipkinUrl, samplingRate);
  }

  public boolean enabled() {
    return tracing != null;
  }

  public HttpServerTracingHandler newServerHandler(boolean tls) {
    if (!enabled()) {
      return null;
    }
    HttpTracing httpTracing = HttpTracing.create(tracing);
    HttpServerTracingDispatch state = new HttpServerTracingDispatch(httpTracing, tls);
    return new HttpServerTracingHandler(state);
  }

  public HttpClientTracingHandler newClientHandler(boolean tls) {
    if (!enabled()) {
      return null;
    }
    HttpTracing httpTracing = HttpTracing.create(tracing);
    HttpClientTracingDispatch tracingDispatch = new HttpClientTracingDispatch(httpTracing, tls);
    return new HttpClientTracingHandler(tracingDispatch);
  }
}
