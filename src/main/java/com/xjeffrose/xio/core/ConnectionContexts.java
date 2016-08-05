package com.xjeffrose.xio.core;


import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class ConnectionContexts {
  private static final AttributeKey<XioConnectionContext> CONNECTION_CONTEXT = AttributeKey.valueOf("XioConnectionContext");

  public static ConnectionContext getContext(Channel channel) {
    ConnectionContext context = channel.pipeline()
        .context(ConnectionContextHandler.class)
        .attr(CONNECTION_CONTEXT).get();

    Preconditions.checkState(context != null, "Context not yet set on channel %s", channel);
    return context;
  }
}
