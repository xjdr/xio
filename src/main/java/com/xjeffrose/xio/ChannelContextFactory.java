package com.xjeffrose.xio;

import java.nio.channels.*;

interface ChannelContextFactory {

  public ChannelContext build(SocketChannel channel);

}
