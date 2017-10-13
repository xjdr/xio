package com.xjeffrose.xio.fixtures;

import com.squareup.okhttp.OkHttpClient;
import com.xjeffrose.xio.SSL.TlsConfig;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.mockwebserver.MockWebServer;

public class OkHttpUnsafe {

  public static KeyManager[] getKeyManagers(TlsConfig config) throws Exception {
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(null, "".toCharArray());
    keystore.setKeyEntry("server", config.getPrivateKey(), "".toCharArray(), config.getCertificateAndTrustChain());
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keystore, "".toCharArray());
    return keyManagerFactory.getKeyManagers();
  }

  public static SSLSocketFactory getUnsafeSSLSocketFactory(KeyManager[] keyManagers) throws NoSuchAlgorithmException, KeyManagementException {
    // Create a trust manager that does not validate certificate chains
    final TrustManager[] trustAllCerts = new TrustManager[]{
      new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }
      }
    };

    // Install the all-trusting trust manager
    final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
    // Create an ssl socket factory with our all-trusting manager
    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
    // System.out.println("supported ciphers: " + java.util.Arrays.asList(sslSocketFactory.getSupportedCipherSuites()));
    // System.out.println("supported SSL parameters ciphers: " + java.util.Arrays.asList(sslContext.getSupportedSSLParameters().getCipherSuites()));
    // System.out.println("supported SSL parameters protocols: " + java.util.Arrays.asList(sslContext.getSupportedSSLParameters().getProtocols()));
    return sslSocketFactory;
  }

  public static OkHttpClient getUnsafeClient() {
    try {
      final SSLSocketFactory sslSocketFactory = getUnsafeSSLSocketFactory(null);

      OkHttpClient okHttpClient = new OkHttpClient();
      okHttpClient.setSslSocketFactory(sslSocketFactory);
      okHttpClient.setHostnameVerifier(new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      });

      return okHttpClient;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static MockWebServer getSslMockWebServer(TlsConfig config) {
    try {
      MockWebServer server = new MockWebServer();
      server.useHttps(OkHttpUnsafe.getUnsafeSSLSocketFactory(getKeyManagers(config)), false);
      return server;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
