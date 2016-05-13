package com.xjeffrose.xio.SSL;

import io.netty.channel.ChannelHandler;
import org.junit.Test;

import static org.junit.Assert.*;

public class XioSecurityHandlerImplTest {

  XioSecurityHandlerImpl xioSecurityHandler = new XioSecurityHandlerImpl();

  @Test
  public void getAuthenticationHandler() throws Exception {
    ChannelHandler auth = xioSecurityHandler.getAuthenticationHandler();

    assertNotNull(auth);
  }

  @Test
  public void getEncryptionHandler() throws Exception {
    ChannelHandler ssl = xioSecurityHandler.getEncryptionHandler();

    assertNotNull(ssl);
  }

}