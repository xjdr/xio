package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import com.xjeffrose.xio.http.internal.FullHttp2Request;
import com.xjeffrose.xio.http.internal.Http2StreamingData;
import com.xjeffrose.xio.http.internal.StreamingHttp2Request;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.PromiseCombiner;

@UnstableApi
public class Http2ServerCodec extends ChannelDuplexHandler {

  private static final AttributeKey<Request> CHANNEL_REQUEST_KEY =
      AttributeKey.newInstance("xio_channel_h2_request");

  private static void setChannelRequest(ChannelHandlerContext ctx, Request request) {
    ctx.channel().attr(CHANNEL_REQUEST_KEY).set(request);
  }

  private static Request getChannelRequest(ChannelHandlerContext ctx) {
    // TODO(CK): Deal with null?
    return ctx.channel().attr(CHANNEL_REQUEST_KEY).get();
  }

  Request wrapHeaders(Http2Headers headers, int streamId, boolean eos) {
    if (eos) {
      return new FullHttp2Request(headers, streamId);
    } else {
      return new StreamingHttp2Request(headers, streamId);
    }
  }

  Request wrapRequest(ChannelHandlerContext ctx, Http2Request msg) {
    if (msg.payload instanceof Http2Headers) {
      Http2Headers headers = (Http2Headers) msg.payload;
      if (msg.eos && headers.method() == null && headers.status() == null) {
        Request request =
            new StreamingRequestData(getChannelRequest(ctx), new Http2StreamingData(headers));
        return request;
      } else {
        Request request = wrapHeaders(headers, msg.streamId, msg.eos);
        setChannelRequest(ctx, request);
        return request;
      }
    } else if (msg.payload instanceof Http2DataFrame) {
      Request request =
          new StreamingRequestData(
              getChannelRequest(ctx),
              new Http2StreamingData(((Http2DataFrame) msg.payload).content(), msg.eos));
      return request;
    }
    // TODO(CK): throw an exception?
    return null;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Http2Request) {
      ctx.fireChannelRead(wrapRequest(ctx, (Http2Request) msg));
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  void writeResponse(ChannelHandlerContext ctx, Response response, ChannelPromise promise) {
    if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    }

    Request request = getChannelRequest(ctx);
    int streamId = request.streamId();
    Http2Headers headers = response.headers().http2Headers();

    headers.status(response.status().codeAsText());

    if (response instanceof FullResponse) {
      setChannelRequest(ctx, null);
      if (response.body().readableBytes() > 0) {
        PromiseCombiner combiner = new PromiseCombiner();
        combiner.add(ctx.write(Http2Response.build(streamId, headers, false), ctx.newPromise()));
        Http2DataFrame data = new DefaultHttp2DataFrame(response.body(), true);
        combiner.add(ctx.write(Http2Response.build(streamId, data, true), ctx.newPromise()));
        combiner.finish(promise);
      } else {
        ctx.write(Http2Response.build(streamId, headers, true), promise);
      }
    } else {
      ctx.write(Http2Response.build(streamId, headers, false), promise);
    }
  }

  void writeContent(ChannelHandlerContext ctx, StreamingData data, ChannelPromise promise) {
    Request request = getChannelRequest(ctx);
    int streamId = request.streamId();
    if (data.endOfMessage()) {
      setChannelRequest(ctx, null);
    }

    boolean dataEos = data.endOfMessage() && data.trailingHeaders().size() == 0;
    Http2Response response =
        Http2Response.build(streamId, new DefaultHttp2DataFrame(data.content(), dataEos), dataEos);

    if (data.trailingHeaders().size() != 0) {
      Http2Headers headers = data.trailingHeaders().http2Headers();
      Http2Response last = Http2Response.build(streamId, headers, true);
      PromiseCombiner combiner = new PromiseCombiner();
      combiner.add(ctx.write(response, ctx.newPromise()));
      combiner.add(ctx.write(last, ctx.newPromise()));
      combiner.finish(promise);
    } else {
      ctx.write(response, promise);
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof StreamingData) {
      writeContent(ctx, (StreamingData) msg, promise);
    } else if (msg instanceof Response) {
      writeResponse(ctx, (Response) msg, promise);
    } else {
      ctx.write(msg, promise);
    }
  }
}
