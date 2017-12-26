package com.xjeffrose.xio.tracing;

import brave.Tracing;
import brave.context.slf4j.MDCCurrentTraceContext;
import brave.http.HttpTracing;
import brave.sampler.Sampler;
import com.xjeffrose.xio.application.ApplicationConfig;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.Reporter;
import zipkin.reporter.okhttp3.OkHttpSender;

public class XioTracing {

  private final Tracing tracing;

  private Reporter<Span> buildReporter(ApplicationConfig config) {
    return AsyncReporter.builder(OkHttpSender.create(config.getZipkinUrl())).build();
  }

  private Tracing buildTracing(ApplicationConfig config) {
    if (config.getZipkinUrl().isEmpty() || !(config.getSamplingRate() > 0f)) {
      return null;
    }
    return Tracing.newBuilder()
        .localServiceName(config.getName())
        // puts trace IDs into logs
        .currentTraceContext(MDCCurrentTraceContext.create())
        .reporter(buildReporter(config))
        .sampler(Sampler.create(config.getSamplingRate()))
        .build();
  }

  public XioTracing(ApplicationConfig config) {
    tracing = buildTracing(config);
  }

  public boolean enabled() {
    return tracing != null;
  }

  public HttpServerTracingHandler newServerHandler(boolean tls) {
    if (!enabled()) {
      return null;
    }
    HttpTracing httpTracing = HttpTracing.create(tracing);
    HttpServerTracingState state = new HttpServerTracingState(httpTracing, tls);
    return new HttpServerTracingHandler(state);
  }

  public HttpClientTracingHandler newClientHandler(boolean tls) {
    if (!enabled()) {
      return null;
    }
    HttpTracing httpTracing = HttpTracing.create(tracing);
    HttpClientTracingState state = new HttpClientTracingState(httpTracing, tls);
    return new HttpClientTracingHandler(state);
  }
}
