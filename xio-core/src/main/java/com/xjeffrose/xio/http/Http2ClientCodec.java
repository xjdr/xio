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
import io.netty.util.concurrent.PromiseCombiner;
import lombok.extern.slf4j.Slf4j;

@UnstableApi
@Slf4j
public class Http2ClientCodec extends ChannelDuplexHandler {

  private Response wrapHeaders(Http2Headers headers, int streamId, boolean eos) {
    if (eos) {
      return new FullHttp2Response(headers, streamId);
    } else {
      return new SegmentedHttp2Response(headers, streamId);
    }
  }

  private Response wrapResponse(ChannelHandlerContext ctx, Http2Response msg) {
    log.debug("wrapResponse msg={}", msg);
    final Response response;
    Http2MessageSession session = Http2MessageSession.lazyCreateSession(ctx);
    if (msg.payload instanceof Http2Headers) {
      Http2Headers headers = (Http2Headers) msg.payload;
      if (msg.eos && headers.method() == null && headers.status() == null) {
        response =
            session
                .currentResponse(msg.streamId)
                .map(
                    resp ->
                        session.onInboundResponse(
                            new SegmentedResponseData(
                                resp, new Http2SegmentedData(headers, msg.streamId))))
                .orElse(null);
      } else {
        response = wrapHeaders(headers, msg.streamId, msg.eos);
        session.onInboundResponse(response);
      }
    } else if (msg.payload instanceof Http2DataFrame) {
      Http2DataFrame frame = (Http2DataFrame) msg.payload;
      response =
          session
              .currentResponse(msg.streamId)
              .map(
                  resp ->
                      session.onInboundResponse(
                          new SegmentedResponseData(
                              resp,
                              new Http2SegmentedData(frame.content(), msg.eos, msg.streamId))))
              .orElse(null);
    } else {
      // TODO(CK): throw an exception?
      response = null;
    }

    return response;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Http2Response) {
      ctx.fireChannelRead(wrapResponse(ctx, (Http2Response) msg));
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private void writeRequest(ChannelHandlerContext ctx, Request request, ChannelPromise promise) {
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

  private void writeContent(ChannelHandlerContext ctx, SegmentedData data, ChannelPromise promise) {
    Headers trailingHeaders = data.trailingHeaders();
    boolean hasTrailing = trailingHeaders != null && trailingHeaders.size() > 0;
    boolean dataEos = data.endOfMessage() && !hasTrailing;
    Http2Request request =
        Http2Request.build(
            data.streamId(), new DefaultHttp2DataFrame(data.content(), dataEos), dataEos);

    if (hasTrailing) {
      Http2Headers headers = trailingHeaders.http2Headers();
      Http2Request last = Http2Request.build(data.streamId(), headers, true);
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
