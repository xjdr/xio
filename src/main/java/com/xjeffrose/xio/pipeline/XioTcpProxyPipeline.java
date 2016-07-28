package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.TcpProxyCodec;
import io.netty.channel.ChannelHandler;

import java.net.InetSocketAddress;

public class XioTcpProxyPipeline extends XioServerPipeline {

  private final InetSocketAddress proxyEndpoint;

  public XioTcpProxyPipeline(InetSocketAddress proxyEndpoint) {
    this.proxyEndpoint = proxyEndpoint;
  }

  public String applicationProtocol() {
    return "tcp-proxy";
  }

  public ChannelHandler getCodecHandler() {
    return new TcpProxyCodec(proxyEndpoint);
  }

}
