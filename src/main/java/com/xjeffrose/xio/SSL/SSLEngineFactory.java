package com.xjeffrose.xio.SSL;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

public class SSLEngineFactory {

  private String PRIVATE_KEY;
  private String X509_CERT;
  private String password;

  public SSLEngineFactory() {
    this.PRIVATE_KEY = null;
    this.X509_CERT = null;
    this.password = null;
  }

  public SSLEngineFactory(String PRIVATE_KEY, String X509_CERT) {

    this.PRIVATE_KEY = PRIVATE_KEY;
    this.X509_CERT = X509_CERT;
    this.password = "passwordsAreGood";
  }

  public SSLEngineFactory(String PRIVATE_KEY, String X509_CERT, String password) {

    this.PRIVATE_KEY = PRIVATE_KEY;
    this.X509_CERT = X509_CERT;
    this.password = password;
  }

  public SSLEngine getEngine() {
    try {
    // Configure SSL.
    KeyStore ks;
    if (PRIVATE_KEY == null) {
      ks = KeyStoreFactory.Generate(SelfSignedX509CertGenerator.generate("example.com"), "passwordsAreGood");
    } else {
      ks = KeyStoreFactory.Generate(X509CertificateGenerator.generate(PRIVATE_KEY, X509_CERT), password);
    }
    KeyManagerFactory kmf;

      kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, "passwordsAreGood".toCharArray());
      SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
      sslCtx.init(kmf.getKeyManagers(), null, null);

      SSLParameters params = new SSLParameters();
      params.setProtocols(new String[]{"TLSv1.2"});

      final SSLEngine engine = sslCtx.createSSLEngine();
      engine.setSSLParameters(params);
      engine.setNeedClientAuth(false);
      engine.setUseClientMode(false);

      return engine;
    } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException |
        CertificateException |IOException | SignatureException | NoSuchProviderException | InvalidKeyException e) {
//      log.severe("Unable to configure the Application Router");
      System.exit(-1);
      throw new RuntimeException(e);
    }
  }

}

