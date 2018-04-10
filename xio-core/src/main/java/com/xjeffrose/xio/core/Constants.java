package com.xjeffrose.xio.core;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Constants<T> {
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  public static final AttributeKey<Channel> CLIENT_OUTBOUND_CHANNEL =
      AttributeKey.valueOf("client_outbound_channel");
  public static final AttributeKey<ConnectionStateTracker> CONNECTION_STATE_TRACKER =
      AttributeKey.valueOf("connection_state_tracker");
  // TODO(JL): Populate value for hard_rate_limited and soft_rate_limited
  public static final AttributeKey<Boolean> HARD_RATE_LIMITED =
      AttributeKey.valueOf("hard_rate_limited");
  public static final AttributeKey<Boolean> SOFT_RATE_LIMITED =
      AttributeKey.valueOf("soft_rate_limited");
  public static final AttributeKey<String> OCC_REQUESTOR_USERNAME =
      AttributeKey.valueOf("occ_requestor_username");
  public final AttributeKey<T> PICKED_OUTBOUND_NODE = AttributeKey.valueOf("picked_outbound_node");
}
