package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import com.xjeffrose.xio.http.internal.FullHttp1Request;
import com.xjeffrose.xio.http.internal.Http1Request;
import com.xjeffrose.xio.http.internal.Http1StreamingData;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;

@UnstableApi
public class Http1ServerCodec extends ChannelDuplexHandler {

  Object wrapRequest(HttpObject msg) {
    if (msg instanceof FullHttpRequest) {
      return new FullHttp1Request((FullHttpRequest)msg);
    } else if (msg instanceof HttpRequest) {
      return new Http1Request((HttpRequest)msg);
    } else if (msg instanceof HttpContent) {
      return new Http1StreamingData((HttpContent)msg);
    }
    // TODO(CK): throw an exception?
    return null;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpObject) {
      ctx.fireChannelRead(wrapRequest((HttpObject)msg));
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  HttpResponse buildResponse(Response response) {
    if (response instanceof FullResponse) {
      FullResponse full = (FullResponse)response;
      ByteBuf content;
      if (full.body() != null) {
        content = full.body();
      } else {
        content = Unpooled.EMPTY_BUFFER;
      }
      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                         full.status(),
                                         content,
                                         full.headers().http1Headers(),
                                         EmptyHttpHeaders.INSTANCE);
    } else {
      return new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                     response.status(),
                                     response.headers().http1Headers());
    }
  }

  HttpContent buildContent(StreamingData data) {
    if (data.endOfStream()) {
      LastHttpContent last = new DefaultLastHttpContent(data.content());
      if (data.trailingHeaders() != null) {
        last.trailingHeaders().add(data.trailingHeaders().http1Headers());
      }
      return last;
    } else {
      return new DefaultHttpContent(data.content());
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof Response) {
      ctx.write(buildResponse((Response)msg), promise);
    } else if (msg instanceof StreamingData) {
      ctx.write(buildContent((StreamingData)msg), promise);
    } else {
      ctx.write(msg, promise);
    }
  }
}
