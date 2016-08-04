package com.xjeffrose.xio.SSL;

import com.typesafe.config.Config;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TlsConfig {

  @Getter
  private final String cert;
  @Getter
  private final String key;

  private static String readPath(String path) {
    try {
      return new String(Files.readAllBytes(Paths.get(path).toAbsolutePath()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public TlsConfig(Config config) {
    cert = config.getString("x509_cert");
    key = config.getString("private_key");
  }

}
