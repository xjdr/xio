package com.xjeffrose.xio.backend.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AsciiString;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpHeaderNames.USER_AGENT;

public class ResponseBuilder {

  private static final ObjectMapper MAPPER;
  private static final AsciiString USER_AGENT_VALUE = AsciiString.of("integration test backend server");
  private static final AsciiString X_ECHO = AsciiString.of("x-echo");
  private static final AsciiString X_METHOD = AsciiString.of("x-method");

  static {
    MAPPER = new ObjectMapper()
      .registerModule(new ParameterNamesModule())
      .registerModule(new Jdk8Module())
      .registerModule(new JavaTimeModule());
  }

  private final ByteBufAllocator allocator;

  private HttpResponseStatus status;
  private ByteBuf body = null;
  private Map<AsciiString, AsciiString> headers = null;

  public ResponseBuilder(ByteBufAllocator allocator) {
    this.allocator = allocator;
  }

  public ResponseBuilder setStatus(HttpResponseStatus status) {
    this.status = status;
    return this;
  }

  public ResponseBuilder setBody(String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    this.body = Unpooled.copiedBuffer(bytes);
    addHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
    return this;
  }

  public ResponseBuilder setBodyData(ByteBuf data) {
    body = data;
    return this;
  }

  public ResponseBuilder setBody(Object pojo) throws IOException {
    body = allocator.directBuffer();
    OutputStream stream = new ByteBufOutputStream(body);
    MAPPER.writeValue(stream, pojo);
    return this;
  }

  public ResponseBuilder addEcho(CharSequence value) {
    return addHeader(X_ECHO, AsciiString.of(value));
  }


  public ResponseBuilder addMethodEcho(CharSequence value) {
    return addHeader(X_METHOD, AsciiString.of(value));
  }

  public ResponseBuilder addHeader(AsciiString key, AsciiString value) {
    if (headers == null) {
      headers = new HashMap<>();
    }
    headers.put(key, value);
    return this;
  }

  public FullHttpResponse buildH1() {
    checkNotNull(status, "HttpResponseStatus not set");
    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buildH2BodyData());
    response.headers().set(USER_AGENT, USER_AGENT_VALUE);
    if (headers != null) {
      headers.forEach((key, value) -> response.headers().set(key, value));
    }
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buildH2BodyData().readableBytes());
    return response;
  }

  public ByteBuf buildH2BodyData() {
    if (body == null) {
      body = Unpooled.EMPTY_BUFFER;
    }
    return body;
  }

  public Http2Headers buildH2Headers() {
    checkNotNull(status, "HttpResponseStatus not set");
    return new DefaultHttp2Headers().status(status.codeAsText());
  }
}
