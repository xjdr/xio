package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import com.xjeffrose.xio.http.internal.FullHttp1Response;
import com.xjeffrose.xio.http.internal.Http1SegmentedData;
import com.xjeffrose.xio.http.internal.SegmentedHttp1Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@UnstableApi
@Slf4j
public class Http1ClientCodec extends ChannelDuplexHandler {

  private static AttributeKey<Integer> CHANNEL_STREAM_ID_KEY =
      AttributeKey.newInstance("xio_channel_stream_id");

  private void setProxiedH2RequestStreamId(ChannelHandlerContext ctx, int streamId) {
    ctx.channel().attr(CHANNEL_STREAM_ID_KEY).set(streamId);
  }

  @Nullable
  private Integer getProxiedH2RequestStreamId(ChannelHandlerContext ctx) {
    return ctx.channel().attr(CHANNEL_STREAM_ID_KEY).get();
  }

  private static final AttributeKey<Response> CHANNEL_RESPONSE_KEY =
      AttributeKey.newInstance("xio_channel_response");

  private static void setChannelResponse(ChannelHandlerContext ctx, Response response) {
    ctx.channel().attr(CHANNEL_RESPONSE_KEY).set(response);
  }

  private static Response getChannelResponse(ChannelHandlerContext ctx) {
    // TODO(CK): Deal with null?
    return ctx.channel().attr(CHANNEL_RESPONSE_KEY).get();
  }

  Response wrapResponse(ChannelHandlerContext ctx, HttpObject msg) {
    log.debug("wrapResponse msg={}", msg);
    Response response = null;
    if (msg instanceof FullHttpResponse) {
      response = new FullHttp1Response((FullHttpResponse) msg);
      setChannelResponse(ctx, response);
    } else if (msg instanceof HttpResponse) {
      response = new SegmentedHttp1Response((HttpResponse) msg);
      setChannelResponse(ctx, response);
    } else if (msg instanceof HttpContent) {
      response =
          new SegmentedResponseData(
              getChannelResponse(ctx), new Http1SegmentedData((HttpContent) msg));
    }
    Integer streamId = getProxiedH2RequestStreamId(ctx);
    if (streamId != null) {
      response = new ProxyResponse(response, streamId);
    }
    // TODO(CK): throw an exception if response is null?
    return response;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpObject) {
      ctx.fireChannelRead(wrapResponse(ctx, (HttpObject) msg));
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  HttpRequest buildRequest(ChannelHandlerContext ctx, Request request) {
    if (request.streamId() != Message.H1_STREAM_ID_NONE) {
      setProxiedH2RequestStreamId(ctx, request.streamId());
    }
    if (!request.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
      request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    }

    if (request.keepAlive()) {
      request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }

    if (request instanceof FullRequest) {
      FullRequest full = (FullRequest) request;
      ByteBuf content = full.body();
      if (content == null) {
        content = Unpooled.EMPTY_BUFFER;
      }
      if (!full.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
        full.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
      }

      // Request request = getChannelRequest(ctx);

      // setChannelResponse(ctx, null);

      return new DefaultFullHttpRequest(
          HttpVersion.HTTP_1_1,
          full.method(),
          full.path(),
          content,
          full.headers().http1Headers(false, true),
          EmptyHttpHeaders.INSTANCE);
    } else {
      // TODO(CK): TransferEncoding
      return new DefaultHttpRequest(
          HttpVersion.HTTP_1_1,
          request.method(),
          request.path(),
          request.headers().http1Headers(false, true));
    }
  }

  HttpContent buildContent(ChannelHandlerContext ctx, SegmentedData data) {
    if (data.endOfMessage()) {
      LastHttpContent last = new DefaultLastHttpContent(data.content());
      if (data.trailingHeaders() != null) {
        last.trailingHeaders().add(data.trailingHeaders().http1Headers(true, true));
      }
      // setChannelRequest(ctx, null);
      return last;
    } else {
      return new DefaultHttpContent(data.content());
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    log.debug("write: msg={}", msg);
    if (msg instanceof SegmentedData) {
      ctx.write(buildContent(ctx, (SegmentedData) msg), promise);
    } else if (msg instanceof Request) {
      log.debug("writing request {}", msg);
      ctx.write(buildRequest(ctx, (Request) msg), promise);
    } else {
      ctx.write(msg, promise);
    }
  }
}
