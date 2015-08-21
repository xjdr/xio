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

import com.xjeffrose.xio.SSL.X509Certificate;
import java.security.KeyStore;
import java.security.cert.Certificate;

public class KeyStoreFactory {

  private KeyStoreFactory() {
  }

  public static KeyStore Generate(X509Certificate cert, String password) {

    final char[] passwd = password.toCharArray();

    try {
      final KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");
      keyStore.load(null, passwd);
      keyStore.setCertificateEntry(cert.getFqdn(), cert.getCert());

      Certificate[] chain = new Certificate[]{cert.getCert()};
      keyStore.setKeyEntry(cert.getFqdn(), cert.getKey(), passwd, chain);

      //For testing only
      //keyStore.store(new FileOutputStream("mykeystore"), passwd);

      return keyStore;
    } catch (Exception e) {
      return null;
    }
  }

}
