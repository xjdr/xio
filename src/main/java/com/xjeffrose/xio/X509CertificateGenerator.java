/*
 *  Copyright (C) 2015 Jeff Rose
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.xjeffrose.xio;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.x509.X509CertImpl;

public final class X509CertificateGenerator {
  private static final Logger log = Logger.getLogger(X509CertificateGenerator.class.getName());

  private X509CertificateGenerator() {
  }

  public static DERKeySpec parseDERKeySpec(String path) {
    try {
      String rawKeyString = new String(Files.readAllBytes(Paths.get(path)));

      // Base64 decode the data
      Base64.Decoder b64decoder = Base64.getDecoder();
      byte[] encoded = b64decoder.decode(
          rawKeyString.replace("-----BEGIN RSA PRIVATE KEY-----\n", "")
              .replace("-----END RSA PRIVATE KEY-----\n", "")
              .replace("\n", "")
      );

      DerInputStream derReader = new DerInputStream(encoded);
      DerValue[] seq = derReader.getSequence(0);

      if (seq.length != 9) {
        throw new RuntimeException(new GeneralSecurityException("Could not parse a PKCS1 private key."));
      }

      DERKeySpec ks = new DERKeySpec();

      ks.version = seq[0].getBigInteger();
      ks.modulus = seq[1].getBigInteger();
      ks.publicExp = seq[2].getBigInteger();
      ks.privateExp = seq[3].getBigInteger();
      ks.prime1 = seq[4].getBigInteger();
      ks.prime2 = seq[5].getBigInteger();
      ks.exp1 = seq[6].getBigInteger();
      ks.exp2 = seq[7].getBigInteger();
      ks.crtCoef = seq[8].getBigInteger();

      return ks;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static PrivateKey buildPrivateKey(DERKeySpec ks) {
    try {
      RSAPrivateCrtKeySpec keySpec = ks.rsaPrivateCrtKeySpec();

      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(keySpec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static PrivateKey parsePrivateKeyFromPEM(String path) {
    DERKeySpec ks = parseDERKeySpec(path);
    return buildPrivateKey(ks);
  }

  public static PublicKey buildPublicKey(DERKeySpec ks) {
    try {
      RSAPublicKeySpec keySpec = ks.rsaPublicKeySpec();

      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePublic(keySpec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

  }

  public static PublicKey parsePublicKeyFromPEM(String path) {
    DERKeySpec ks = parseDERKeySpec(path);
    return buildPublicKey(ks);
  }

  public static X509Certificate generate(String keyPath, String certPath) {
    try {
      DERKeySpec ks = parseDERKeySpec(keyPath);
      PrivateKey privateKey = buildPrivateKey(ks);
      PublicKey publicKey = buildPublicKey(ks);

      // Sign the cert to identify the algorithm that's used.
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      java.security.cert.X509Certificate x509Certificate = (java.security.cert.X509Certificate) cf.generateCertificate(new FileInputStream(certPath));
      X509CertImpl cert = (X509CertImpl) x509Certificate;

      //cert.sign(privateKey, "SHA1withRSA");
//      cert.verify(publicKey);

      return new X509Certificate(cert.getIssuerX500Principal().getName(), privateKey, cert);
    } catch (FileNotFoundException | CertificateException e) {
      log.log(Priority.ERROR, "Failed to import x509 cert", e);
      throw new RuntimeException(e);
    }
  }

  static class DERKeySpec {
    public BigInteger version;
    public BigInteger modulus;
    public BigInteger publicExp;
    public BigInteger privateExp;
    public BigInteger prime1;
    public BigInteger prime2;
    public BigInteger exp1;
    public BigInteger exp2;
    public BigInteger crtCoef;

    public RSAPrivateCrtKeySpec rsaPrivateCrtKeySpec() {
      return new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);
    }

    public RSAPublicKeySpec rsaPublicKeySpec() {
      return new RSAPublicKeySpec(modulus, publicExp);
    }
  }
}
