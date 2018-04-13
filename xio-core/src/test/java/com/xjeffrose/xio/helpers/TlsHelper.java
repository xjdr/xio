package com.xjeffrose.xio.helpers;

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import java.security.KeyStore;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

public class TlsHelper {

  /**
   * Creates a {@link KeyManager[]} for use in the test utility {@link
   * OkHttpUnsafe#getSslMockWebServer(KeyManager[])}
   */
  public static KeyManager[] getKeyManagers() throws Exception {
    TlsConfig tlsConfig = TlsConfig.fromConfig("xio.tlsConfig", ConfigFactory.load());
    return getKeyManagers(tlsConfig);
  }

  /**
   * Creates a {@link KeyManager[]} from a {@link TlsConfig} for use in the test utility {@link
   * OkHttpUnsafe#getSslMockWebServer(KeyManager[])}
   */
  public static KeyManager[] getKeyManagers(TlsConfig config) throws Exception {
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(null, "".toCharArray());
    keystore.setKeyEntry(
        "server", config.getPrivateKey(), "".toCharArray(), config.getCertificateAndChain());
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keystore, "".toCharArray());
    return keyManagerFactory.getKeyManagers();
  }
}
