package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import com.xjeffrose.xio.http.internal.FullHttp1Request;
import com.xjeffrose.xio.http.internal.Http1Request;
import com.xjeffrose.xio.http.internal.Http1StreamingData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;

@UnstableApi
public class Http1ServerCodec extends ChannelDuplexHandler {

  private static final AttributeKey<Request> CHANNEL_REQUEST_KEY =
      AttributeKey.newInstance("xio_channel_request");

  private static void setChannelRequest(ChannelHandlerContext ctx, Request request) {
    ctx.channel().attr(CHANNEL_REQUEST_KEY).set(request);
  }

  private static Request getChannelRequest(ChannelHandlerContext ctx) {
    // TODO(CK): Deal with null?
    return ctx.channel().attr(CHANNEL_REQUEST_KEY).get();
  }

  Request wrapRequest(ChannelHandlerContext ctx, HttpObject msg) {
    if (msg instanceof FullHttpRequest) {
      Request request = new FullHttp1Request((FullHttpRequest) msg);
      setChannelRequest(ctx, request);
      return request;
    } else if (msg instanceof HttpRequest) {
      Request request = new Http1Request((HttpRequest) msg);
      setChannelRequest(ctx, request);
      return request;
    } else if (msg instanceof HttpContent) {
      Request request =
          new StreamingRequestData(
              getChannelRequest(ctx), new Http1StreamingData((HttpContent) msg));
      return request;
    }
    // TODO(CK): throw an exception?
    return null;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpObject) {
      ctx.fireChannelRead(wrapRequest(ctx, (HttpObject) msg));
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  HttpResponse buildResponse(ChannelHandlerContext ctx, Response response) {
    if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    }

    Request request = getChannelRequest(ctx);
    if (request.keepAlive()) {
      response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }

    if (response instanceof FullResponse) {
      FullResponse full = (FullResponse) response;
      ByteBuf content;
      if (full.body() != null) {
        content = full.body();
      } else {
        content = Unpooled.EMPTY_BUFFER;
      }
      if (!full.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
        full.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
      }

      setChannelRequest(ctx, null);

      return new DefaultFullHttpResponse(
          HttpVersion.HTTP_1_1,
          full.status(),
          content,
          full.headers().http1Headers(),
          EmptyHttpHeaders.INSTANCE);
    } else {
      // TODO(CK): TransferEncoding
      // We don't know the size of the message payload so set TransferEncoding to chunked
      if (!response.headers().contains(HttpHeaderNames.TRANSFER_ENCODING)) {
        response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
      }

      return new DefaultHttpResponse(
          HttpVersion.HTTP_1_1, response.status(), response.headers().http1Headers());
    }
  }

  HttpContent buildContent(ChannelHandlerContext ctx, StreamingData data) {
    if (data.endOfStream()) {
      LastHttpContent last = new DefaultLastHttpContent(data.content());
      if (data.trailingHeaders() != null) {
        last.trailingHeaders().add(data.trailingHeaders().http1Headers());
      }
      setChannelRequest(ctx, null);
      return last;
    } else {
      return new DefaultHttpContent(data.content());
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof StreamingData) {
      ctx.write(buildContent(ctx, (StreamingData) msg), promise);
    } else if (msg instanceof Response) {
      ctx.write(buildResponse(ctx, (Response) msg), promise);
    } else {
      ctx.write(msg, promise);
    }
  }
}
