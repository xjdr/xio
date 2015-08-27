package com.xjeffrose.xio.SSL;

import java.security.KeyStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeyStoreFactoryTest {

  @Test
  public void testGenerateSelfSigned() throws Exception {
    KeyStore ks = ks = KeyStoreFactory.Generate(SelfSignedX509CertGenerator.generate("example.com"), "selfsignedcert");

    assertEquals(ks.size(), 1);
  }

  @Test
  public void testGenerateRealCerts() throws Exception {
    KeyStore ks = KeyStoreFactory.Generate(X509CertificateGenerator.generate("src/test/resources/privateKey.pem", "src/test/resources/cert.pem"), "my_really-AWESOME_P@ss");

    assertEquals(ks.size(), 1);
  }
}