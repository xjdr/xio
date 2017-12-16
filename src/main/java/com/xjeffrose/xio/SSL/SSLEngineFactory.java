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
import java.security.cert.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLEngineFactory {

  private final String PRIVATE_KEY;
  private final String X509_CERT;
  private final String password;
  private final boolean client;

  private KeyStore ks = null;
  private KeyStore ts = null;
  private SSLContext sslCtx;
  private SSLParameters params;

  public SSLEngineFactory() {
    this(false);
  }

  public SSLEngineFactory(boolean client) {
    this.PRIVATE_KEY = null;
    this.X509_CERT = null;
    this.password = null;
    this.client = client;
  }

  public SSLEngineFactory(String PRIVATE_KEY, String X509_CERT) {
    this(PRIVATE_KEY, X509_CERT, "passwordsAreGood");
  }

  public SSLEngineFactory(String PRIVATE_KEY, String X509_CERT, String password) {

    this.PRIVATE_KEY = PRIVATE_KEY;
    this.X509_CERT = X509_CERT;
    this.password = password;
    this.client = false;
  }

  private TrustManager[] trustAnyone() {
    return new TrustManager[] {
      new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
      }
    };
  }

  public SSLEngine getEngine() {
    try {
      if (client) {
        sslCtx = SSLContext.getInstance("TLSv1.2");
        sslCtx.init(null, trustAnyone(), null);

        params = new SSLParameters();
        params.setProtocols(new String[] {"TLSv1.2"});
      } else {
        // Configure SSL.
        if (PRIVATE_KEY == null) {
          ks =
              KeyStoreFactory.Generate(
                  SelfSignedX509CertGenerator.generate("example.com"), "passwordsAreGood");
        } else {
          ks =
              KeyStoreFactory.Generate(
                  X509CertificateGenerator.generate(PRIVATE_KEY, X509_CERT), password);
        }
        KeyManagerFactory kmf;

        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "passwordsAreGood".toCharArray());
        sslCtx = SSLContext.getInstance("TLSv1.2");
        sslCtx.init(kmf.getKeyManagers(), null, null);

        params = new SSLParameters();
        params.setProtocols(new String[] {"TLSv1.2"});
      }

      final SSLEngine engine = sslCtx.createSSLEngine();
      engine.setSSLParameters(params);
      engine.setNeedClientAuth(false);
      engine.setUseClientMode(client);

      return engine;
    } catch (NoSuchAlgorithmException
        | UnrecoverableKeyException
        | KeyStoreException
        | KeyManagementException
        | CertificateException
        | IOException
        | SignatureException
        | NoSuchProviderException
        | InvalidKeyException e) {
      //      log.severe("Unable to configure the SSLEngine");
      System.exit(-1);
      throw new RuntimeException(e);
    }
  }
}
