/*
 * Copyright (C) 2015 Jeff Rose
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
 */
package com.xjeffrose.xio.tls;

import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Date;

public final class SelfSignedX509CertGenerator {

  static final Date NOT_BEFORE = new Date(System.currentTimeMillis() - 86400000L * 365);
  static final Date NOT_AFTER = new Date(253402300799000L);

  SelfSignedX509CertGenerator() {}

  public static X509Certificate generate(String fqdn)
      throws IOException, CertificateException, NoSuchProviderException, NoSuchAlgorithmException,
          InvalidKeyException, SignatureException {

    SelfSignedCertificate selfSignedCertificate =
        new SelfSignedCertificate(fqdn, NOT_BEFORE, NOT_AFTER);

    return new X509Certificate(fqdn, selfSignedCertificate.key(), selfSignedCertificate.cert());
  }
}
