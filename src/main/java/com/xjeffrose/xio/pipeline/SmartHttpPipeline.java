package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.http.ApplicationCodecPlaceholderHandler;
import com.xjeffrose.xio.http.CodecPlaceholderHandler;
import com.xjeffrose.xio.http.GentleSslHandler;
import com.xjeffrose.xio.http.Http2HandlerBuilder;
import com.xjeffrose.xio.http.HttpNegotiationHandler;
import com.xjeffrose.xio.http.HttpsUpgradeHandler;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

/**
 * SmartHttpPipeline does http well.
 * If configured with a certificate pair (which it should be by default), it will:
 *   - accept incoming TLS and negotiate the application protocol
 *     - supporting http/2 requests if the client can
 *     - supporting http/1 requests as a fallback
 *   - try to upgrade plain text requests with http response code 426
 *   - http/1.1 over cleartext if configured to do so (not the default)
 *
 */
@Slf4j
public class SmartHttpPipeline extends XioServerPipeline {

  private final XioPipelineFragment fragment;

  public SmartHttpPipeline() {
    fragment = null;
  }

  public SmartHttpPipeline(XioPipelineFragment fragment) {
    this.fragment = fragment;
  }

  public SmartHttpPipeline(XioChannelHandlerFactory factory) {
    this.fragment = new XioSimplePipelineFragment(factory);
  }

  public String applicationProtocol() {
    return "ssl-http/1.1";
  }

  public ChannelHandler buildHttp2Handler() {
    return new Http2HandlerBuilder().server(true).build();
  }

  public ChannelHandler getCodecNegotiationHandler(XioServerConfig config) {
    if (config.getTls().isUseSsl()) {
      return new HttpNegotiationHandler(this::buildHttp2Handler);
    } else {
      return null;
    }
  }

  public ChannelHandler getCodecHandler(XioServerConfig config) {
    if (config.getTls().isUseSsl()) {
      return CodecPlaceholderHandler.INSTANCE;
    } else {
      return new HttpServerCodec();
    }
  }

  public ChannelHandler getEncryptionHandler(XioServerConfig config, XioServerState state) {
    if (config.getTls().isUseSsl()) {
      return new GentleSslHandler(state.getSslContext(), new HttpsUpgradeHandler());
    } else {
      return null;
    }
  }

  public ChannelHandler getApplicationCodec() {
    return ApplicationCodecPlaceholderHandler.INSTANCE;
  }

  public void buildHandlers(ApplicationState appState, XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
    super.buildHandlers(appState, config, state, pipeline);
    if (fragment != null) {
      fragment.buildHandlers(appState, config, state, pipeline);
    }
  }
}
