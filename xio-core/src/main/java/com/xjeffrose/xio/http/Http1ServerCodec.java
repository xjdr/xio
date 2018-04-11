package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import com.xjeffrose.xio.http.internal.FullHttp1Request;
import com.xjeffrose.xio.http.internal.Http1SegmentedData;
import com.xjeffrose.xio.http.internal.SegmentedHttp1Request;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@UnstableApi
@Slf4j
public class Http1ServerCodec extends ChannelDuplexHandler {

  private static final AttributeKey<Http1MessageSession> CHANNEL_MESSAGE_SESSION_KEY =
      AttributeKey.newInstance("xio_channel_h1_message_session");

  private static Http1MessageSession setDefaultMessageSession(ChannelHandlerContext ctx) {
    Http1MessageSession session = ctx.channel().attr(CHANNEL_MESSAGE_SESSION_KEY).get();
    if (session == null) {
      session = new Http1MessageSession();
      ctx.channel().attr(CHANNEL_MESSAGE_SESSION_KEY).set(session);
    }
    return session;
  }

  /** Wrap the HttpObject with the appropriate type and fire read on the next handler. */
  private void wrapRequest(ChannelHandlerContext ctx, HttpObject msg) {
    Http1MessageSession session = setDefaultMessageSession(ctx);
    try {
      Request request;
      if (msg instanceof FullHttpRequest) {
        request = new FullHttp1Request((FullHttpRequest) msg);
        session.onRequest(request);
      } else if (msg instanceof HttpRequest) {
        request = new SegmentedHttp1Request((HttpRequest) msg);
        session.onRequest(request);
      } else if (msg instanceof HttpContent) {
        SegmentedData data = new Http1SegmentedData((HttpContent) msg);
        session.onRequestData(data);
        Request sessionRequest = session.currentRequest();
        if (sessionRequest == null) {
          // We don't have a sessionRequest so we can't construct a SegmentedRequestData.
          // Don't log as session.onRequestData should have logged.
          return;
        }
        request = new SegmentedRequestData(sessionRequest, data);
      } else {
        log.error("Dropping unsupported http object: {}", msg);
        return;
      }

      ctx.fireChannelRead(request);
    } finally {
      session.flush();
    }
  }

  /** Handles instances of HttpObject, all other types are forwarded to the next handler. */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpObject) {
      wrapRequest(ctx, (HttpObject) msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  /** Translate the Response object into a netty HttpResponse and fire write on the next handler. */
  private void buildResponse(ChannelHandlerContext ctx, Response response, ChannelPromise promise) {
    Http1MessageSession session = setDefaultMessageSession(ctx);
    try {
      session.onResponse(response);
      Request request = session.currentRequest();

      if (request != null) {
        if (request.keepAlive()) {
          response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
      } else {
        // We don't have a request object associated with this
        // session, so we can't determine the appropriate KeepAlive
        // header. Don't log as session.onResponse should have logged.
        return;
      }

      HttpObject obj;

      if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
      }

      boolean closeConnection = session.closeConnection() && (response instanceof FullResponse);
      if (session.closeConnection()) {
        response.headers().set(HttpHeaderNames.CONNECTION, "close");
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

        obj =
            new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                full.status(),
                content,
                full.headers().http1Headers(false, false),
                EmptyHttpHeaders.INSTANCE);
      } else {
        // TODO(CK): TransferEncoding
        // We don't know the size of the message payload so set TransferEncoding to chunked
        if (!response.headers().contains(HttpHeaderNames.TRANSFER_ENCODING)) {
          response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        }

        obj =
            new DefaultHttpResponse(
                HttpVersion.HTTP_1_1,
                response.status(),
                response.headers().http1Headers(false, false));
      }

      ChannelFuture future = ctx.write(obj, promise);
      if (closeConnection) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
    } finally {
      session.flush();
    }
  }

  /**
   * Translate the SegmentedData object into a netty HttpContent and fire write on the next handler.
   */
  private void buildContent(ChannelHandlerContext ctx, SegmentedData data, ChannelPromise promise) {
    Http1MessageSession session = setDefaultMessageSession(ctx);
    try {
      session.onResponseData(data);
      HttpObject obj;

      if (data.endOfMessage()) {
        LastHttpContent last = new DefaultLastHttpContent(data.content());
        if (data.trailingHeaders() != null) {
          last.trailingHeaders().add(data.trailingHeaders().http1Headers(true, false));
        }
        obj = last;
      } else {
        obj = new DefaultHttpContent(data.content());
      }

      ChannelFuture future = ctx.write(obj, promise);
      if (session.closeConnection() && data.endOfMessage()) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
    } finally {
      session.flush();
    }
  }

  /**
   * Handles instances of SegmentedData and Response, all other types are forwarded to the next
   * handler.
   */
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof SegmentedData) {
      buildContent(ctx, (SegmentedData) msg, promise);
    } else if (msg instanceof Response) {
      buildResponse(ctx, (Response) msg, promise);
    } else {
      ctx.write(msg, promise);
    }
  }
}
