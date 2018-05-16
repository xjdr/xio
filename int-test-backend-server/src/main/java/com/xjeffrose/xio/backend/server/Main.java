package com.xjeffrose.xio.backend.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import java.net.InetAddress;
import java.util.Arrays;

import static com.xjeffrose.xio.test.OkHttpUnsafe.getKeyManagers;
import static okhttp3.Protocol.HTTP_1_1;
import static okhttp3.Protocol.HTTP_2;

@Slf4j
public class Main {

  public static void main(String args[]) throws Exception {
    if (args.length < 3) {
      throw new RuntimeException("please specify server 'host' and 'port' and 'header-tag' arguments");
    }
    
    // header-tag might be the ip address of this host or any other information you
    // would like to use to identify the traffic served up by this host
    val headerPropKey = "header-tag";
    val host = args[0];
    val port = args[1];
    val taggedHeaderValue = args[2];

    Config config = ConfigFactory.load();
    TlsConfig tlsConfig = new TlsConfig(config);
    val keyManagers = getKeyManagers(tlsConfig.getPrivateKey(), tlsConfig.getCertificateAndChain());
    val server = OkHttpUnsafe.getSslMockWebServer(keyManagers);
    val protocols = Arrays.asList(HTTP_2, HTTP_1_1);
    server.setProtocols(protocols);
    server.setDispatcher(
        new Dispatcher() {
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            log.debug("dispatching response for request: {}", request);
            return new MockResponse()
                .addHeader(headerPropKey, taggedHeaderValue)
                .setBody("Release the Kraken")
                .setSocketPolicy(SocketPolicy.KEEP_OPEN);
          }
        });

    server.start(InetAddress.getByName(host), Integer.parseInt(port));
  }
}
