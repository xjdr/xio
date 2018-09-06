package com.xjeffrose.xio.tracing;

import brave.Tracing;
import brave.context.slf4j.MDCCurrentTraceContext;
import brave.opentracing.BraveTracer;
import brave.sampler.Sampler;
import com.xjeffrose.xio.config.TracingConfig;
import datadog.opentracing.DDTracer;
import io.opentracing.Tracer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import zipkin2.Span;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpClientBuilderFactory;
import zipkin2.reporter.okhttp3.OkHttpSender;
import zipkin2.reporter.okhttp3.OkHttpSenderBuilderFactory;

@Slf4j
public class XioTracing {

  private Tracer tracer;
  private final String name;

  public XioTracing(TracingConfig config) {
    name = config.getApplicationName();
    TracingConfig.TracingType type = config.getType();

    switch (type) {
      case ZIPKIN:
        String zipkinUrl = config.getZipkinUrl();
        float samplingRate = config.getZipkinSamplingRate();
        Tracing tracing = buildZipkinTracing(this.name, zipkinUrl, samplingRate);
        if (tracing != null) {
          tracer = BraveTracer.create(tracing);
        }
        break;
      case DATADOG:
        tracer = new DDTracer();
        break;
    }
    log.info("Configured tracer type: {}", type.toString());
  }

  public boolean enabled() {
    return tracer != null;
  }

  public HttpServerTracingHandler newServerHandler() {
    if (!enabled()) {
      return null;
    }
    HttpServerTracingDispatch state = new HttpServerTracingDispatch(name, tracer);
    return new HttpServerTracingHandler(state);
  }

  public HttpClientTracingHandler newClientHandler() {
    if (!enabled()) {
      return null;
    }
    HttpClientTracingDispatch tracingDispatch = new HttpClientTracingDispatch(name, tracer);
    return new HttpClientTracingHandler(tracingDispatch);
  }

  Reporter<Span> buildReporter(@NonNull String zipkinUrl) {
    OkHttpClient.Builder clientBuilder = OkHttpClientBuilderFactory.createZipkinClientBuilder();
    OkHttpSender sender =
        OkHttpSenderBuilderFactory.createSenderBuilder(clientBuilder)
            .encoding(Encoding.JSON)
            .endpoint(zipkinUrl)
            .compressionEnabled(false)
            .build();
    return AsyncReporter.builder(sender).build();
  }

  Tracing buildZipkinTracing(@NonNull String name, @NonNull String zipkinUrl, float samplingRate) {
    if (zipkinUrl.isEmpty() || !(samplingRate > 0f)) {
      return null;
    }
    return Tracing.newBuilder()
        .localServiceName(name)
        // puts trace IDs into logs
        .currentTraceContext(MDCCurrentTraceContext.create())
        .spanReporter(buildReporter(zipkinUrl))
        .sampler(Sampler.create(samplingRate))
        .build();
  }
}
