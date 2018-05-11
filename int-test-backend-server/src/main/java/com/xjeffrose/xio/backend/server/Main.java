package com.xjeffrose.xio.backend.server;

import static com.google.common.base.Preconditions.checkNotNull;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import lombok.val;
import okhttp3.mockwebserver.*;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.Optional;

import static okhttp3.Protocol.HTTP_1_1;
import static okhttp3.Protocol.HTTP_2;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

public class Main {

  public static void main(String args[]) throws Exception {
    val headerPropKey = "header-tag";
    val portPropKey = "port";
    val taggedHeaderValue = "todo"; //System.getProperty(headerPropKey);
    val port = "8080"; //System.getProperty(portPropKey);

    checkNotNull(taggedHeaderValue, headerPropKey);
    checkNotNull(port, portPropKey);

    val keyManagers = createKeyManager();
    val server = OkHttpUnsafe.getSslMockWebServer(keyManagers);
    val protocols = Arrays.asList(HTTP_2, HTTP_1_1);
    server.setProtocols(protocols);
    server.setDispatcher(
        new Dispatcher() {
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            String index = Optional.ofNullable(request.getHeader("x_index")).orElse("unknown");
            return new MockResponse()
                .addHeader(headerPropKey, taggedHeaderValue)
                .setBody("Release the Kraken")
                .setSocketPolicy(SocketPolicy.KEEP_OPEN);
          }
        });

    server.start(Integer.parseInt(port));
  }

  private static KeyManager[] createKeyManager() throws Exception {
    Config config = ConfigFactory.load();
    TlsConfig tlsConfig = new TlsConfig(config);

    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(null, "".toCharArray());
    keystore.setKeyEntry(
        "server", tlsConfig.getPrivateKey(), "".toCharArray(), tlsConfig.getCertificateAndChain());
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keystore, "".toCharArray());
    KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
    return keyManagers;
  }
}
