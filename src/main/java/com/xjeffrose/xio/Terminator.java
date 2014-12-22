package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.security.*;
import java.util.logging.*;

import javax.net.ssl.*;

import com.xjeffrose.log.*;

class Terminator {
  private static final Logger log = Log.getLogger(Terminator.class.getName());
  private final SocketChannel channel;
  private SSLEngine engine;

  Terminator(SocketChannel channel) {
    this.channel = channel;
    ByteBuffer peerNetData = ByteBuffer.allocate(1024);

    engine = getEngine();

  }

  private SSLEngine getEngine() {
    try {
      SSLContext sslctx = SSLContext.getInstance("TLSv1.2");
      sslctx.init(null,null,null);
      SSLEngine engine = sslctx.createSSLEngine();
      engine.setUseClientMode(false);
    } catch (KeyManagementException|NoSuchAlgorithmException e) {}
    return engine;
  }

}

