package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class XioSslHttp1_1Pipeline extends XioHttp1_1Pipeline {
  private static final Logger log = LoggerFactory.getLogger(XioSslHttp1_1Pipeline.class);

  public XioSslHttp1_1Pipeline() {
    super();
  }

  public XioSslHttp1_1Pipeline(XioPipelineFragment fragment) {
    super(fragment);
  }

  public XioSslHttp1_1Pipeline(XioChannelHandlerFactory factory) {
    super(factory);
  }

  public String applicationProtocol() {
    return "ssl-http/1.1";
  }

  public ChannelHandler getEncryptionHandler() {
    try {
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      SslContext sslCtx =  SslContextBuilder
        .forServer(ssc.certificate(), ssc.privateKey())
        .sslProvider(SslProvider.OPENSSL)
        .build();

      return sslCtx.newHandler(new PooledByteBufAllocator());

    } catch (SSLException | CertificateException e) {
      log.error("Couldn't build encryption handler", e);
      return null;
    }
  }

}
