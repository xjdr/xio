package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.SSL.XioSecurityHandlerImpl;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelHandler;

public class XioTlsServerPipeline extends XioServerPipeline {

  public XioTlsServerPipeline() {
  }

  @Override
  public ChannelHandler getEncryptionHandler(XioServerConfig config, XioServerState state) {
    return new XioSecurityHandlerImpl(config.getCert(), config.getKey()).getEncryptionHandler();
  }

}
