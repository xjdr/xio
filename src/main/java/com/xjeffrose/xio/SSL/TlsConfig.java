package com.xjeffrose.xio.SSL;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.typesafe.config.Config;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import javax.xml.bind.DatatypeConverter;
import lombok.Getter;

public class TlsConfig {

  @Getter
  private final boolean useSsl;
  // used internally
  private final boolean useOpenSsl;
  @Getter
  private final boolean logInsecureConfig;
  @Getter
  private final PrivateKey privateKey;
  // custom getter
  private final X509Certificate certificate;
  @Getter
  private final List<String> x509TrustChain;
  @Getter
  private final ApplicationProtocolConfig alpnConfig;
  // custom getter
  private final List<String> ciphers;
  @Getter
  private final ClientAuth clientAuth;
  @Getter
  private final boolean enableOcsp;
  // custom getter
  private final List<String> protocols;
  @Getter
  private final long sessionCacheSize;
  @Getter
  private final long sessionTimeout;

  private static String readPath(String path) {
    try {
      return new String(Files.readAllBytes(Paths.get(path).toAbsolutePath()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String readUrlResource(URL url) {
    try {
      URLConnection connection = url.openConnection();

      connection.connect();

      InputStream stream = connection.getInputStream();

      return CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8));
    } catch (IOException e) {
      return null;
    }
  }

  private static String readClasspathResource(String resource) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      throw new RuntimeException("null class loader");
    }

    URL url = loader.getResource(resource);
    if (url == null) {
      throw new RuntimeException("resource not found on classpath: " + resource);
    }

    return readUrlResource(url);
  }

  private static String readPathFromKey(String key, Config config) {
    String value = config.getString(key);
    if (value.startsWith("classpath:")) {
      return readClasspathResource(value.replace("classpath:", ""));
    }
    if (value.startsWith("url:")) {
      try {
        URL url = new URL(value.replace("url:", ""));
        return readUrlResource(url);
      } catch (MalformedURLException e) {
        return null;
      }
    }
    return readPath(value);
  }

  /**
   * Only works with PKCS8 formatted private keys
   */
  private static PrivateKey parsePrivateKeyFromPem(String pemData) {
    PrivateKey key = null;
    try {
      StringBuilder builder = new StringBuilder();
      boolean inKey = false;
      for (String line : pemData.split("\n")) {
        if (!inKey) {
          if (line.startsWith("-----BEGIN ") && line.endsWith(" PRIVATE KEY-----")) {
            inKey = true;
          }
          continue;
        } else {
          if (line.startsWith("-----END ") && line.endsWith(" PRIVATE KEY-----")) {
            inKey = false;
            break;
          }
          builder.append(line);
        }
      }

      byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      key = kf.generatePrivate(keySpec);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
    return key;
  }

  private static X509Certificate parseX509CertificateFromPem(String pemData) {
    try {
      CertificateFactory fact = CertificateFactory.getInstance("X.509");
      ByteArrayInputStream is = new ByteArrayInputStream(pemData.getBytes(StandardCharsets.UTF_8));
      X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
      return cer;
    } catch (CertificateException e) {
      return null;
    }
  }

  private static ApplicationProtocolConfig buildAlpnConfig(Config config) {
    ApplicationProtocolConfig.Protocol protocol = config.getEnum(ApplicationProtocolConfig.Protocol.class, "protocol");
    ApplicationProtocolConfig.SelectorFailureBehavior selectorBehavior = config.getEnum(ApplicationProtocolConfig.SelectorFailureBehavior.class, "selectorBehavior");
    ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedBehavior = config.getEnum(ApplicationProtocolConfig.SelectedListenerFailureBehavior.class, "selectedBehavior");
    List<String> supportedProtocols = config.getStringList("supportedProtocols");
    return new ApplicationProtocolConfig(
                                         protocol,
                                         selectorBehavior,
                                         selectedBehavior,
                                         supportedProtocols
                                         );
  }

  public TlsConfig(Config config) {
    useSsl = config.getBoolean("useSsl");
    logInsecureConfig = config.getBoolean("logInsecureConfig");
    privateKey = parsePrivateKeyFromPem(readPathFromKey("privateKeyPath", config));
    certificate = parseX509CertificateFromPem(readPathFromKey("x509CertPath", config));
    x509TrustChain = null;
    useOpenSsl = config.getBoolean("useOpenSsl");
    alpnConfig = buildAlpnConfig(config.getConfig("alpn"));
    ciphers = config.getStringList("ciphers");
    clientAuth = config.getEnum(ClientAuth.class, "clientAuth");
    enableOcsp = config.getBoolean("enableOcsp");
    protocols = config.getStringList("protocols");
    sessionCacheSize = config.getLong("sessionCacheSize");
    sessionTimeout = config.getLong("sessionTimeout");
  }

  public List<String> getCiphers() {
    if (ciphers.size() == 0) {
      return null;
    }
    return ciphers;
  }

  public String[] getProtocols() {
    if (protocols.size() == 0) {
      return null;
    }
    return protocols.toArray(new String[0]);
  }

  public X509Certificate[] getCertificateAndTrustChain() {
    return Lists.asList(certificate, new X509Certificate[0]).toArray(new X509Certificate[0]);
  }

  public SslProvider getSslProvider() {
    if (useOpenSsl) {
      if (!OpenSsl.isAvailable()) {
        throw new IllegalStateException("useOpenSsl = true and OpenSSL is not available");
      }
      return SslProvider.OPENSSL;
    }
    return SslProvider.JDK;
  }
}
