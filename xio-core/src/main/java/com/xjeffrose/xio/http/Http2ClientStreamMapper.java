package com.xjeffrose.xio.http;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.util.AttributeKey;

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
      streamMap.put(id, mappedId);
    }
    return mappedId;
  }

  public Integer inboundStreamId(Integer id, boolean remove) {
    Integer mappedId;
    if (remove) {
      mappedId = streamMap.inverse().remove(id);
    } else {
      mappedId = streamMap.inverse().get(id);
    }
    return mappedId == null ? id : mappedId;
  }

  public void clear() {
    streamMap.clear();
  }
}
