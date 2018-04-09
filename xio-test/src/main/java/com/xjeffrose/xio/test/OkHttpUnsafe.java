package com.xjeffrose.xio.test;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Create okhttp clients/servers using an unsafe trust manager for testing purposes.
 *
 * <p><strong>NOTE:</strong> Never use the objects created by this class in production. They are
 * purely for testing purposes, and thus very insecure.
 */
public class OkHttpUnsafe {

  public static KeyManager[] getKeyManagers(
      PrivateKey privateKey, X509Certificate[] certificateAndChain) throws Exception {
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(null, "".toCharArray());
    keystore.setKeyEntry("server", privateKey, "".toCharArray(), certificateAndChain);
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keystore, "".toCharArray());
    return keyManagerFactory.getKeyManagers();
  }

  public static KeyManager[] getEmptyKeyManager() throws Exception {
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(null, "".toCharArray());
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keystore, "".toCharArray());
    return keyManagerFactory.getKeyManagers();
  }

  public static X509TrustManager unsafeTrustManager() {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
          throws CertificateException {}

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
          throws CertificateException {}

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };
  }

  public static SSLSocketFactory getUnsafeSSLSocketFactory(
      KeyManager[] keyManagers, X509TrustManager trustManager)
      throws NoSuchAlgorithmException, KeyManagementException {
    // Create a trust manager that does not validate certificate chains
    final TrustManager[] trustAllCerts = new TrustManager[] {trustManager};

    // Install the all-trusting trust manager
    final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
    // Create an ssl socket factory with our all-trusting manager
    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
    // System.out.println("supported ciphers: " +
    // java.util.Arrays.asList(sslSocketFactory.getSupportedCipherSuites()));
    // System.out.println("supported SSL parameters ciphers: " +
    // java.util.Arrays.asList(sslContext.getSupportedSSLParameters().getCipherSuites()));
    // System.out.println("supported SSL parameters protocols: " +
    // java.util.Arrays.asList(sslContext.getSupportedSSLParameters().getProtocols()));
    return sslSocketFactory;
  }

  public static OkHttpClient getUnsafeClient() throws Exception {
    X509TrustManager trustManager = unsafeTrustManager();
    final SSLSocketFactory sslSocketFactory = getUnsafeSSLSocketFactory(null, trustManager);

    OkHttpClient okHttpClient =
        new OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustManager)
            .hostnameVerifier(
                new HostnameVerifier() {
                  @Override
                  public boolean verify(String hostname, SSLSession session) {
                    return true;
                  }
                })
            .protocols(Arrays.asList(Protocol.HTTP_1_1))
            .build();

    return okHttpClient;
  }

  public static MockWebServer getSslMockWebServer(KeyManager[] keyManagers) throws Exception {
    MockWebServer server = new MockWebServer();
    server.useHttps(
        OkHttpUnsafe.getUnsafeSSLSocketFactory(keyManagers, unsafeTrustManager()), false);
    return server;
  }

  public static MockWebServer getSslMockWebServer(
      PrivateKey privateKey, X509Certificate[] certificateAndChain) throws Exception {
    return getSslMockWebServer(getKeyManagers(privateKey, certificateAndChain));
  }
}
