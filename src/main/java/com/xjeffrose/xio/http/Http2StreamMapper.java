package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelDuplexHandler;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

@Slf4j
public class Http2StreamMapper extends ChannelDuplexHandler {


  private static final AttributeKey<BiMap<Integer, Integer>> STREAM_ID_BI_MAP_KEY = AttributeKey.newInstance("xio_stream_id_bi_map_key");

  public static BiMap<Integer, Integer> getIdMap(ChannelHandlerContext ctx) {
    BiMap<Integer, Integer> idMap = ctx.channel().attr(STREAM_ID_BI_MAP_KEY).get();
    if (idMap == null) {
      idMap = HashBiMap.create();
      ctx.channel().attr(STREAM_ID_BI_MAP_KEY).set(idMap);
    }
    return idMap;
  }

  public static void setIdMapping(ChannelHandlerContext ctx, int inRequest, int outRequest) {
    getIdMap(ctx).forcePut(inRequest, outRequest);
  }

  public static int getOutboundResponseId(ChannelHandlerContext ctx, int inResponse) {
    BiMap<Integer, Integer> idMap = ctx.channel().attr(STREAM_ID_BI_MAP_KEY).get();
    if (idMap == null) {
      throw new RuntimeException("Couldn't find the bi-map for channel: " + ctx.channel());
    }
    Integer responseId = idMap.inverse().get(inResponse);

    if (responseId == null) {
      throw new RuntimeException("Coudln't find the outbound response stream id for inbound response stream id: " + inResponse);
    }

    return responseId;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Http2Response) {
      Http2Response response = (Http2Response)msg;
      ctx.fireChannelRead(response.newStreamId(getOutboundResponseId(ctx, response.streamId)));
    } else {
      ctx.fireChannelRead(msg);
    }

  }
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof Http2Request) {
      Http2Request request = (Http2Request)msg;

      // this assumes that there is an Http2Handler further down the pipeline
      ctx.write(request.payload, promise);
      // get the outbound stream id from the Http2Handler
      setIdMapping(ctx, request.streamId, Http2Handler.getCurrentStreamId(ctx));
    } else {
      ctx.write(msg, promise);
    }
  }

}
