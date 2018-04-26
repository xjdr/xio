package com.xjeffrose.xio.http;

import static com.xjeffrose.xio.http.Http2MessageSession.lazyCreateSession;

import com.xjeffrose.xio.core.internal.UnstableApi;
import com.xjeffrose.xio.http.internal.FullHttp2Request;
import com.xjeffrose.xio.http.internal.Http2SegmentedData;
import com.xjeffrose.xio.http.internal.SegmentedHttp2Request;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.concurrent.PromiseCombiner;

@UnstableApi
public class Http2ServerCodec extends ChannelDuplexHandler {

  Request wrapHeaders(Http2Headers headers, int streamId, boolean eos) {
    if (eos) {
      return new FullHttp2Request(headers, streamId);
    } else {
      return new SegmentedHttp2Request(headers, streamId);
    }
  }

  private Request wrapRequest(ChannelHandlerContext ctx, Http2Request msg) {
    Http2MessageSession messageSession = lazyCreateSession(ctx);
    if (msg.payload instanceof Http2Headers) {
      Http2Headers headers = (Http2Headers) msg.payload;
      if (msg.eos && headers.method() == null && headers.status() == null) {
        Request initialRequest = messageSession.currentRequest(msg.streamId);
        if (initialRequest != null) {
          SegmentedRequestData request =
              new SegmentedRequestData(
                  initialRequest, new Http2SegmentedData(headers, msg.streamId));
          messageSession.onInboundRequest(request);
          return request;
        }
      } else {
        Request request = wrapHeaders(headers, msg.streamId, msg.eos);
        messageSession.onInboundRequest(request);
        return request;
      }
    } else if (msg.payload instanceof Http2DataFrame) {
      Http2DataFrame frame = (Http2DataFrame) msg.payload;
      Request initialRequest = messageSession.currentRequest(msg.streamId);
      if (initialRequest != null) {
        SegmentedRequestData data =
            new SegmentedRequestData(
                initialRequest, new Http2SegmentedData(frame.content(), msg.eos, msg.streamId));
        messageSession.onInboundRequestData(data);
        return data;
      }
    }
    // TODO(CK): throw an exception?
    return null;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Http2Request) {
      Http2Request request = (Http2Request) msg;
      ctx.fireChannelRead(wrapRequest(ctx, request));
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private void writeResponse(ChannelHandlerContext ctx, Response response, ChannelPromise promise) {
    if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    }

    Http2MessageSession messageSession = lazyCreateSession(ctx);
    int streamId = response.streamId();
    Http2Headers headers = response.headers().http2Headers();

    headers.status(response.status().codeAsText());

    if (response instanceof FullResponse) {
      messageSession.onOutboundResponse(response);
      ByteBuf body = response.body();
      if (body != null && body.readableBytes() > 0) {
        PromiseCombiner combiner = new PromiseCombiner();
        combiner.add(ctx.write(Http2Response.build(streamId, headers, false), ctx.newPromise()));
        Http2DataFrame data = new DefaultHttp2DataFrame(body, true);
        combiner.add(ctx.write(Http2Response.build(streamId, data, true), ctx.newPromise()));
        combiner.finish(promise);
      } else {
        ctx.write(Http2Response.build(streamId, headers, true), promise);
      }
    } else {
      ctx.write(Http2Response.build(streamId, headers, false), promise);
    }

    messageSession.flush(streamId);
  }

  void writeContent(ChannelHandlerContext ctx, SegmentedData data, ChannelPromise promise) {
    Http2MessageSession messageSession = lazyCreateSession(ctx);
    messageSession.onOutboundResponseData(data);

    boolean dataEos = data.endOfMessage() && data.trailingHeaders().size() == 0;
    Http2Response response =
        Http2Response.build(
            data.streamId(), new DefaultHttp2DataFrame(data.content(), dataEos), dataEos);

    Headers trailingHeaders = data.trailingHeaders();
    if (trailingHeaders != null && trailingHeaders.size() != 0) {
      Http2Headers headers = trailingHeaders.http2Headers();
      Http2Response last = Http2Response.build(data.streamId(), headers, true);
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
    if (msg instanceof SegmentedData) {
      writeContent(ctx, (SegmentedData) msg, promise);
    } else if (msg instanceof Response) {
      writeResponse(ctx, (Response) msg, promise);
    } else {
      ctx.write(msg, promise);
    }
  }
}
