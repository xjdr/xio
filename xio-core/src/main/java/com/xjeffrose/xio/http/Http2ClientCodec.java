package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import com.xjeffrose.xio.http.internal.FullHttp2Response;
import com.xjeffrose.xio.http.internal.Http2SegmentedData;
import com.xjeffrose.xio.http.internal.SegmentedHttp2Response;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.PromiseCombiner;
import lombok.extern.slf4j.Slf4j;

@UnstableApi
@Slf4j
public class Http2ClientCodec extends ChannelDuplexHandler {

  private static final AttributeKey<Response> CHANNEL_RESPONSE_KEY =
      AttributeKey.newInstance("xio_channel_h2_response");

  private static void setChannelResponse(ChannelHandlerContext ctx, Response response) {
    ctx.channel().attr(CHANNEL_RESPONSE_KEY).set(response);
  }

  private static Response getChannelResponse(ChannelHandlerContext ctx) {
    // TODO(CK): Deal with null?
    return ctx.channel().attr(CHANNEL_RESPONSE_KEY).get();
  }

  Response wrapHeaders(Http2Headers headers, int streamId, boolean eos) {
    if (eos) {
      return new FullHttp2Response(headers, streamId);
    } else {
      return new SegmentedHttp2Response(headers, streamId);
    }
  }

  Response wrapResponse(ChannelHandlerContext ctx, Http2Response msg) {
    log.debug("wrapResponse msg={}", msg);
    if (msg.payload instanceof Http2Headers) {
      Http2Headers headers = (Http2Headers) msg.payload;
      if (msg.eos && headers.method() == null && headers.status() == null) {
        return new SegmentedResponseData(
            getChannelResponse(ctx), new Http2SegmentedData(headers, msg.streamId));
      } else {
        Response response = wrapHeaders(headers, msg.streamId, msg.eos);
        setChannelResponse(ctx, response);
        return response;
      }
    } else if (msg.payload instanceof Http2DataFrame) {
      return new SegmentedResponseData(
          getChannelResponse(ctx),
          new Http2SegmentedData(((Http2DataFrame) msg.payload).content(), msg.eos, msg.streamId));
    }
    // TODO(CK): throw an exception?
    return null;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Http2Response) {
      ctx.fireChannelRead(wrapResponse(ctx, (Http2Response) msg));
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  void writeRequest(ChannelHandlerContext ctx, Request request, ChannelPromise promise) {
    /*
      // TOOD(CK): define ACCEPT?
    if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    }
    */

    Http2Headers headers = request.headers().http2Headers();

    headers.authority(request.host()).method(request.method().asciiName()).path(request.path());

    int streamId = request.streamId();

    if (request instanceof FullRequest) {
      if (request.body().readableBytes() > 0) {
        PromiseCombiner combiner = new PromiseCombiner();
        combiner.add(ctx.write(Http2Request.build(streamId, headers, false), ctx.newPromise()));
        Http2DataFrame data = new DefaultHttp2DataFrame(request.body(), true);
        combiner.add(ctx.write(Http2Request.build(streamId, data, true), ctx.newPromise()));
        combiner.finish(promise);
      } else {
        ctx.write(Http2Request.build(streamId, headers, true), promise);
      }
    } else {
      ctx.write(Http2Request.build(streamId, headers, false), promise);
    }
  }

  void writeContent(ChannelHandlerContext ctx, SegmentedData data, ChannelPromise promise) {
    int streamId = 0; // TODO(CK): need a no stream constant somewhere
    boolean dataEos = data.endOfMessage() && data.trailingHeaders().size() == 0;
    Http2Request request =
        Http2Request.build(streamId, new DefaultHttp2DataFrame(data.content(), dataEos), dataEos);

    if (data.trailingHeaders().size() != 0) {
      Http2Headers headers = data.trailingHeaders().http2Headers();
      Http2Request last = Http2Request.build(streamId, headers, true);
      PromiseCombiner combiner = new PromiseCombiner();
      combiner.add(ctx.write(request, ctx.newPromise()));
      combiner.add(ctx.write(last, ctx.newPromise()));
      combiner.finish(promise);
    } else {
      ctx.write(request, promise);
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    log.debug("write: msg={}", msg);
    if (msg instanceof SegmentedData) {
      writeContent(ctx, (SegmentedData) msg, promise);
    } else if (msg instanceof Request) {
      writeRequest(ctx, (Request) msg, promise);
    } else {
      ctx.write(msg, promise);
    }
  }
}
