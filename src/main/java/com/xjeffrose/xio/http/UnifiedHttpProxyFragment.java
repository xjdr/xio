package com.xjeffrose.xio.http;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelPipeline;

public class UnifiedHttpProxyFragment implements XioPipelineFragment {

  private final UrlRouter router;
  private final Http2UrlRouter h2Router;

  public UnifiedHttpProxyFragment(UrlRouter router, Http2UrlRouter h2Router) {
    this.router = router;
    this.h2Router = h2Router;
  }

  public String applicationProtocol() {
    return null;
  }

  public void buildHandlers(ApplicationState appState, XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
    pipeline.addLast(new Http1ProxyHandler(router));
    pipeline.addLast(new Http2ProxyHandler(h2Router));
  }

}
