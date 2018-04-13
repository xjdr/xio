package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import static com.xjeffrose.xio.http.Http2MessageSession.contextMessageSession;
import com.xjeffrose.xio.http.internal.FullHttp2Request;
import com.xjeffrose.xio.http.internal.Http2SegmentedData;
import com.xjeffrose.xio.http.internal.SegmentedHttp2Request;
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

  Request wrapRequest(ChannelHandlerContext ctx, Http2Request msg) {
    Http2MessageSession messageSession = contextMessageSession(ctx);
    if (msg.payload instanceof Http2Headers) {
      Http2Headers headers = (Http2Headers) msg.payload;
      if (msg.eos && headers.method() == null && headers.status() == null) {
        //todo: WBK deal with null
        SegmentedRequestData request =  new SegmentedRequestData(messageSession.currentRequest(msg.streamId),
          new Http2SegmentedData(headers, msg.streamId));
        messageSession.onRequest(request);
        return request;
      } else {
        Request request = wrapHeaders(headers, msg.streamId, msg.eos);
        messageSession.onRequest(request);
        return request;
      }
    } else if (msg.payload instanceof Http2DataFrame) {
      Http2DataFrame frame = (Http2DataFrame) msg.payload;
      SegmentedRequestData data =  new SegmentedRequestData(
        messageSession.currentRequest(msg.streamId),
        new Http2SegmentedData(frame.content(), msg.eos, msg.streamId));
      messageSession.onRequestData(data);
      return data;
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

  void writeResponse(ChannelHandlerContext ctx, Response response, ChannelPromise promise) {
    if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    }

    Http2MessageSession messageSession = contextMessageSession(ctx);
    int streamId = response.streamId();
    Http2Headers headers = response.headers().http2Headers();

    headers.status(response.status().codeAsText());

    if (response instanceof FullResponse) {
      messageSession.onResponse(response);
      if (response.body().readableBytes() > 0) {
        PromiseCombiner combiner = new PromiseCombiner();
        combiner.add(ctx.write(Http2Response.build(streamId, headers, false), ctx.newPromise()));
        Http2DataFrame data = new DefaultHttp2DataFrame(response.body() , true);
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
    Http2MessageSession messageSession = contextMessageSession(ctx);
    messageSession.onResponseData(data);

//    Request request = messageSession.currentRequest(data.streamId());
//    if (data.endOfMessage()) { //todo: WBK we probably don't need to do this any longer
//      setChannelRequest(ctx, null);
//    }

    boolean dataEos = data.endOfMessage() && data.trailingHeaders().size() == 0;
    Http2Response response =
      Http2Response.build(data.streamId(), new DefaultHttp2DataFrame(data.content(), dataEos), dataEos);

    if (data.trailingHeaders().size() != 0) {
      Http2Headers headers = data.trailingHeaders().http2Headers();
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
