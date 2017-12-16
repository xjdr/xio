package com.xjeffrose.xio.SSL;

import static org.junit.Assert.*;

import io.netty.channel.ChannelHandler;
import org.junit.Test;

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

  @Test
  public void generateX509() throws Exception {
    X509Certificate selfSignedCert = SelfSignedX509CertGenerator.generate("*.paypal.com");
    assertNotNull(selfSignedCert);
  }
}
