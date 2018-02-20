package com.xjeffrose.xio.client.asyncretry;

import io.netty.channel.EventLoopGroup;

public interface AsyncRetryLoopFactory {
  public AsyncRetryLoop buildLoop(EventLoopGroup eventLoopGroup);
}
