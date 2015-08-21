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

package com.xjeffrose.xio.SSL;

import java.security.PrivateKey;
import java.util.Date;
import org.apache.log4j.Logger;
import sun.security.x509.X509CertImpl;

public final class X509Certificate {
  static final Date NOT_BEFORE = new Date(System.currentTimeMillis() - 86400000L * 365);
  static final Date NOT_AFTER = new Date(253402300799000L);
  private static final Logger log = Logger.getLogger(X509Certificate.class.getName());
  private final String fqdn;

//  private final File certificate;
//  private final File privateKey;
  private final PrivateKey key;
  private final X509CertImpl cert;
  public X509Certificate(String fqdn, PrivateKey key, X509CertImpl cert) {

    this.fqdn = fqdn;
    this.key = key;
    this.cert = cert;
  }

  public String getFqdn() {
    return fqdn;
  }

  public PrivateKey getKey() {
    return key;
  }

  public X509CertImpl getCert() {
    return cert;
  }
}
