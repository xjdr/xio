package com.xjeffrose.xio.pipeline;

import io.netty.channel.ChannelHandler;

import java.util.List;

abstract public class XioPipelineFragment {

  abstract public List<ChannelHandler> buildHandlers();

}
