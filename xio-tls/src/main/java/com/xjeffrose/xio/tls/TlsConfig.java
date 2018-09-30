package com.xjeffrose.xio.tls;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.Getter;

public class TlsConfig {
  @Getter private final boolean useSsl;
  @Getter private final SslProvider sslProvider;
  @Getter private final boolean logInsecureConfig;
  @Getter private final PrivateKey privateKey;
  // custom getter
  private final List<X509Certificate> trustedCerts;
  // custom getter
  private final List<X509Certificate> certificateAndChain;
  @Getter private final ApplicationProtocolConfig alpnConfig;
  // custom getter
  private final List<String> ciphers;
  @Getter private final ClientAuth clientAuth;
  @Getter private final boolean enableOcsp;
  // custom getter
  private final List<String> protocols;
  @Getter private final long sessionCacheSize;
  @Getter private final long sessionTimeout;

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

  private static String readPathFromValue(String value) {
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

  /** Only works with PKCS8 formatted private keys */
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

      byte[] encoded = BaseEncoding.base64().decode(builder.toString());
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
    ApplicationProtocolConfig.Protocol protocol =
        config.getEnum(ApplicationProtocolConfig.Protocol.class, "protocol");
    ApplicationProtocolConfig.SelectorFailureBehavior selectorBehavior =
        config.getEnum(ApplicationProtocolConfig.SelectorFailureBehavior.class, "selectorBehavior");
    ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedBehavior =
        config.getEnum(
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.class, "selectedBehavior");
    List<String> supportedProtocols = config.getStringList("supportedProtocols");
    return new ApplicationProtocolConfig(
        protocol, selectorBehavior, selectedBehavior, supportedProtocols);
  }

  private static List<X509Certificate> buildCerts(List<String> paths) {
    List<X509Certificate> certificates = new ArrayList<>();

    for (String path : paths) {
      certificates.add(parseX509CertificateFromPem(readPathFromValue(path)));
    }

    return certificates;
  }

  /**
   * Returns a value from a config that might not exist.
   *
   * @param config the config to fetch a value out of
   * @param path the path to fetch
   * @param configGetter the function to fetch the value out of the config, if the path exists
   */
  private static <T> Optional<T> getOptionalPath(
      Config config, String path, BiFunction<Config, String, T> configGetter) {
    if (config.hasPath(path)) {
      return Optional.of(configGetter.apply(config, path));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Creates a new builder with values from the given config object.
   *
   * @throws {@link com.typesafe.config.ConfigException} if any of the keys present in the Config
   *     object have unexpected value types.
   */
  public static Builder builderFrom(Config config) {
    Builder builder = TlsConfig.builder();
    getOptionalPath(config, "useSsl", Config::getBoolean).map(builder::useSsl);
    getOptionalPath(config, "logInsecureConfig", Config::getBoolean)
        .map(builder::logInsecureConfig);

    getOptionalPath(config, "x509TrustedCertPaths", Config::getStringList)
        .map(TlsConfig::buildCerts)
        .map(builder::trustedCerts);

    getOptionalPath(config, "privateKeyPath", Config::getString)
        .map(TlsConfig::readPathFromValue)
        .map(TlsConfig::parsePrivateKeyFromPem)
        .map(builder::privateKey);

    getOptionalPath(config, "x509CertPath", Config::getString)
        .map(TlsConfig::readPathFromValue)
        .map(TlsConfig::parseX509CertificateFromPem)
        .map(builder::certificate);

    getOptionalPath(config, "x509CertChainPaths", Config::getStringList)
        .map(TlsConfig::buildCerts)
        .map(builder::certChain);

    getOptionalPath(config, "useOpenSsl", Config::getBoolean).map(builder::useOpenSsl);

    getOptionalPath(config, "alpn", Config::getConfig)
        .map(TlsConfig::buildAlpnConfig)
        .map(builder::alpnConfig);

    getOptionalPath(config, "ciphers", Config::getStringList).map(builder::ciphers);
    getOptionalPath(
            config, "clientAuth", (configArg, key) -> configArg.getEnum(ClientAuth.class, key))
        .map(builder::clientAuth);
    getOptionalPath(config, "enableOcsp", Config::getBoolean).map(builder::enableOcsp);
    getOptionalPath(config, "protocols", Config::getStringList).map(builder::protocols);
    getOptionalPath(config, "sessionCacheSize", Config::getLong).map(builder::sessionCacheSize);
    getOptionalPath(config, "sessionTimeout", Config::getLong).map(builder::sessionTimeout);

    return builder;
  }

  @lombok.Builder(builderClassName = "Builder")
  private TlsConfig(
      Boolean useSsl,
      Boolean useOpenSsl,
      Boolean logInsecureConfig,
      PrivateKey privateKey,
      X509Certificate certificate,
      List<X509Certificate> certChain,
      List<X509Certificate> trustedCerts,
      ApplicationProtocolConfig alpnConfig,
      List<String> ciphers,
      ClientAuth clientAuth,
      Boolean enableOcsp,
      List<String> protocols,
      Long sessionCacheSize,
      Long sessionTimeout) {
    // Validate that *all* parameters were set in the builder.
    String errorMessage = "{} must be set in builder";
    Preconditions.checkNotNull(useSsl, errorMessage, "useSsl");
    Preconditions.checkNotNull(useOpenSsl, errorMessage, "useOpenSsl");
    Preconditions.checkNotNull(logInsecureConfig, errorMessage, "logInsecureConfig");
    Preconditions.checkNotNull(privateKey, errorMessage, "privateKey");
    Preconditions.checkNotNull(certificate, errorMessage, "certificate");
    Preconditions.checkNotNull(certChain, errorMessage, "certChain");
    Preconditions.checkNotNull(trustedCerts, errorMessage, "trustedCerts");
    Preconditions.checkNotNull(alpnConfig, errorMessage, "alpnConfig");
    Preconditions.checkNotNull(ciphers, errorMessage, "ciphers");
    Preconditions.checkNotNull(clientAuth, errorMessage, "clientAuth");
    Preconditions.checkNotNull(enableOcsp, errorMessage, "enableOcsp");
    Preconditions.checkNotNull(protocols, errorMessage, "protocols");
    Preconditions.checkNotNull(sessionCacheSize, errorMessage, "sessionCacheSize");
    Preconditions.checkNotNull(sessionTimeout, errorMessage, "sessionTimeout");

    this.useSsl = useSsl;
    if (useOpenSsl) {
      if (!OpenSsl.isAvailable()) {
        throw new IllegalStateException("useOpenSsl = true and OpenSSL is not available");
      }
      this.sslProvider = SslProvider.OPENSSL;
    } else {
      this.sslProvider = SslProvider.JDK;
    }
    this.logInsecureConfig = logInsecureConfig;
    this.privateKey = privateKey;
    this.trustedCerts = trustedCerts;
    this.certificateAndChain =
        ImmutableList.<X509Certificate>builder().add(certificate).addAll(certChain).build();
    this.alpnConfig = alpnConfig;
    this.ciphers = ciphers;
    this.clientAuth = clientAuth;
    this.enableOcsp = enableOcsp;
    this.protocols = protocols;
    this.sessionCacheSize = sessionCacheSize;
    this.sessionTimeout = sessionTimeout;
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

  public X509Certificate[] getCertificateAndChain() {
    return certificateAndChain.toArray(new X509Certificate[0]);
  }

  public X509Certificate[] getTrustedCerts() {
    return trustedCerts.toArray(new X509Certificate[0]);
  }
}
