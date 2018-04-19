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
import lombok.extern.slf4j.Slf4j;

@UnstableApi
@Slf4j
public class Http1ClientCodec extends ChannelDuplexHandler {

  private static AttributeKey<Http2To1ProxyRequestQueue> CHANNEL_PROXY_REQUEST_QUEUE =
      AttributeKey.newInstance("xio_channel_proxy_request_queue");

  private Http2To1ProxyRequestQueue getProxyRequestQueue(ChannelHandlerContext ctx) {
    Http2To1ProxyRequestQueue queue = ctx.channel().attr(CHANNEL_PROXY_REQUEST_QUEUE).get();
    if (queue == null) {
      queue = new Http2To1ProxyRequestQueue();
      ctx.channel().attr(CHANNEL_PROXY_REQUEST_QUEUE).set(queue);
    }
    return queue;
  }

  Response wrapResponse(ChannelHandlerContext ctx, HttpObject msg) {
    log.debug("wrapResponse msg={}", msg);
    final Response response;
    if (msg instanceof FullHttpResponse) {
      response = new FullHttp1Response((FullHttpResponse) msg);
    } else if (msg instanceof HttpResponse) {
      response = new SegmentedHttp1Response((HttpResponse) msg);
    } else if (msg instanceof HttpContent) {
      response =
          getProxyRequestQueue(ctx)
              .currentResponse()
              .map(r -> new SegmentedResponseData(r, new Http1SegmentedData((HttpContent) msg)))
              .orElse(null);
    } else {
      // TODO(CK): throw an exception if response is null?
      response = null;
    }

    return getProxyRequestQueue(ctx)
        .currentProxiedH2StreamId()
        .<Response>map(streamId -> new ProxyResponse(response, streamId))
        .orElse(response);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpObject) {
      Response response = wrapResponse(ctx, (HttpObject) msg);
      ctx.fireChannelRead(response);
      getProxyRequestQueue(ctx).onResponseDrainNext(ctx, response);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  HttpRequest buildRequest(Request request) {
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

  HttpContent buildContent(SegmentedData data) {
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
      SegmentedData segmentedData = (SegmentedData) msg;
      HttpContent content = buildContent(segmentedData);
      getProxyRequestQueue(ctx)
          .onRequestWriteOrEnqueue(ctx, segmentedData.streamId(), content, promise);
    } else if (msg instanceof Request) {
      Request request = (Request) msg;
      HttpRequest message = buildRequest(request);
      getProxyRequestQueue(ctx).onRequestWriteOrEnqueue(ctx, request.streamId(), message, promise);
    } else {
      ctx.write(msg, promise);
    }
  }
}
