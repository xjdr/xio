package com.xjeffrose.xio.http;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2ClientStreamMapper {

  static final AttributeKey<Http2ClientStreamMapper> H2_CLIENT_STREAM_MAPPER =
      AttributeKey.newInstance("H2_CLIENT_STREAM_MAPPER");

  public static Http2ClientStreamMapper http2ClientStreamMapper(ChannelHandlerContext ctx) {
    Http2ClientStreamMapper mapper = ctx.channel().attr(H2_CLIENT_STREAM_MAPPER).get();
    if (mapper == null) {
      mapper = new Http2ClientStreamMapper();
      ctx.channel().attr(H2_CLIENT_STREAM_MAPPER).set(mapper);
    }
    return mapper;
  }

  private Http2ClientStreamMapper() {}

  private final BiMap<Integer, Integer> streamMap = HashBiMap.create();

  public Integer outboundStreamId(Http2Connection connection, Integer id) {
    Integer mappedId = streamMap.get(id);
    if (mappedId == null) {
      mappedId = connection.local().incrementAndGetNextStreamId();
      log.debug("h2 client outbound stream id {} : {}", id, mappedId);
      streamMap.put(id, mappedId);
    }
    return mappedId;
  }

  public Integer inboundStreamId(Integer id, boolean remove) {
    Integer mappedId;
    if (remove) {
      mappedId = streamMap.inverse().remove(id);
      log.debug("h2 client inbound stream id {} : {}", id, mappedId);
    } else {
      mappedId = streamMap.inverse().get(id);
      log.debug("h2 client inbound stream id {} : {}", id, mappedId);
    }
    return mappedId == null ? id : mappedId;
  }

  public void clear() {
    log.debug("h2 client stream mapping clear");
    streamMap.clear();
  }
}
