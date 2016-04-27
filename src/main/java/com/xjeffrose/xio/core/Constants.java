package com.xjeffrose.xio.core;

import com.xjeffrose.xio.client.loadbalancer.Node;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class Constants {
  public static final AttributeKey<ConnectionStateTracker> CONNECTION_STATE_TRACKER = AttributeKey.valueOf("connection_state_tracker");
  public static final AttributeKey<Channel> CLIENT_OUTBOUND_CHANNEL = AttributeKey.valueOf("client_outbound_channel");
  public static final AttributeKey<String> OCC_REQUESTOR_USERNAME = AttributeKey.valueOf("occ_requestor_username");
  public static final AttributeKey<Node> PICKED_OUTBOUND_NODE = AttributeKey.valueOf("picked_outbound_node");

}
