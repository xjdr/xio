package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.TcpProxyCodec;
import com.xjeffrose.xio.server.XioServerConfig;
import io.netty.channel.ChannelHandler;
import java.net.InetSocketAddress;

public class XioTcpProxyPipeline extends XioServerPipeline {

  private final InetSocketAddress proxyEndpoint;

  public XioTcpProxyPipeline(InetSocketAddress proxyEndpoint) {
    this.proxyEndpoint = proxyEndpoint;
  }

  @Override
  public String applicationProtocol() {
    return "tcp-proxy";
  }

  @Override
  public ChannelHandler getCodecHandler(XioServerConfig config) {
    return new TcpProxyCodec(proxyEndpoint);
  }

}
