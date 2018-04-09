package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Recipes {
  protected Recipes() {}

  private static final HttpVersion v1_1 = HttpVersion.HTTP_1_1;

  public static enum ContentType {
    Application_Json("application/json"),
    Text_Plain("text/plain"),
    Text_Html("text/html");

    private final String value;

    ContentType(String value) {
      this.value = value;
    }
  }

  public static ByteBuf unpooledBuffer(String payload) {
    return Unpooled.copiedBuffer(payload.getBytes(StandardCharsets.UTF_8));
  }

  // Request {{{
  public static FullHttpRequest newFullRequest(
      HttpMethod method, String urlPath, ByteBuf buffer, ContentType contentType) {
    FullHttpRequest request =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, urlPath, buffer);
    request.headers().set(CONTENT_TYPE, contentType.value);
    request.headers().setInt(CONTENT_LENGTH, buffer.readableBytes());
    return request;
  }

  public static HttpRequest newRequestDelete(String urlPath) {
    return new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, urlPath);
  }

  public static HttpRequest newRequestGet(String urlPath) {
    return new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, urlPath);
  }

  public static HttpRequest newRequestPost(
      String urlPath, ByteBuf buffer, ContentType contentType) {
    return newFullRequest(HttpMethod.POST, urlPath, buffer, contentType);
  }

  public static HttpRequest newRequestPost(
      String urlPath, String payload, ContentType contentType) {
    return newRequestPost(urlPath, unpooledBuffer(payload), contentType);
  }

  public static HttpRequest newRequestPut(String urlPath, ByteBuf buffer, ContentType contentType) {
    return newFullRequest(HttpMethod.PUT, urlPath, buffer, contentType);
  }

  public static HttpRequest newRequestPut(String urlPath, String payload, ContentType contentType) {
    return newRequestPut(urlPath, unpooledBuffer(payload), contentType);
  }
  // Request }}}

  // Response {{{
  public static HttpResponse newResponse(HttpResponseStatus status) {
    return new DefaultHttpResponse(v1_1, status);
  }

  public static HttpResponse newResponse(
      HttpResponseStatus status, ByteBuf buffer, ContentType contentType) {
    FullHttpResponse response = new DefaultFullHttpResponse(v1_1, status, buffer);

    response.headers().set(CONTENT_TYPE, contentType.value);
    response.headers().setInt(CONTENT_LENGTH, buffer.readableBytes());

    return response;
  }

  public static HttpResponse newResponse(
      HttpResponseStatus status, String payload, ContentType contentType) {
    return newResponse(status, unpooledBuffer(payload), contentType);
  }

  // OK {{{
  public static HttpResponse newResponseOk() {
    return newResponse(HttpResponseStatus.OK);
  }

  public static HttpResponse newResponseOk(String payload) {
    return newResponse(HttpResponseStatus.OK, payload, ContentType.Text_Plain);
  }

  public static HttpResponse newResponseOk(String payload, ContentType contentType) {
    return newResponse(HttpResponseStatus.OK, payload, contentType);
  }
  // OK }}}

  // BAD_REQUEST {{{
  public static HttpResponse newResponseBadRequest() {
    return newResponse(HttpResponseStatus.BAD_REQUEST);
  }

  public static HttpResponse newResponseBadRequest(String payload) {
    return newResponse(HttpResponseStatus.BAD_REQUEST, payload, ContentType.Text_Plain);
  }

  public static HttpResponse newResponseBadRequest(String payload, ContentType contentType) {
    return newResponse(HttpResponseStatus.BAD_REQUEST, payload, contentType);
  }
  // BAD_REQUEST }}}
  // Response }}}

  // useful for both encoders and decoders
  public static List<ByteBuf> extractBuffers(EmbeddedChannel channel) {
    channel.runPendingTasks();
    List<ByteBuf> buffers = Lists.newArrayList();
    while (true) {
      ByteBuf buffer = channel.readOutbound();
      if (buffer == null) {
        break;
      }
      buffers.add(buffer);
    }
    return buffers;
  }

  // encoders {{{
  public static List<ByteBuf> encodeRequest(DefaultFullHttpRequest request) {
    EmbeddedChannel channel = new EmbeddedChannel();

    channel.pipeline().addLast("http request encoder", new HttpRequestEncoder());
    channel.writeOutbound(request);
    return extractBuffers(channel);
  }

  public static List<ByteBuf> encodeResponse(HttpResponse response) {
    EmbeddedChannel channel = new EmbeddedChannel();

    channel.pipeline().addLast("http response encoder", new HttpResponseEncoder());
    channel.writeOutbound(response);
    return extractBuffers(channel);
  }
  // encoders }}}

  // decoders {{{

  public static HttpRequest decodeRequest(List<ByteBuf> payload) {
    EmbeddedChannel channel = new EmbeddedChannel();

    channel
        .pipeline()
        .addLast("http request decoder", new HttpRequestDecoder())
        .addLast("http message aggregator", new HttpObjectAggregator(1048576));

    for (ByteBuf buffer : payload) {
      channel.writeInbound(buffer);
    }

    HttpRequest request = channel.readInbound();
    return request;
  }

  public static HttpResponse decodeResponse(List<ByteBuf> payload) {
    EmbeddedChannel channel = new EmbeddedChannel();

    channel
        .pipeline()
        .addLast("http response decoder", new HttpResponseDecoder())
        .addLast("http message aggregator", new HttpObjectAggregator(1048576));

    for (ByteBuf buffer : payload) {
      channel.writeInbound(buffer);
    }

    HttpResponse response = channel.readInbound();
    return response;
  }
  // decoders }}}

}
