/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.management.web.utils;

import com.terracotta.management.security.SSLContextFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author Ludovic Orban
 */
public class TSASslContextFactory implements SSLContextFactory {

  @Override
  public SSLContext create() throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException, URISyntaxException {
    SSLContext sslCtxt = SSLContext.getInstance("TLS");

    TrustManager[] trustManagers = null;
    if (Boolean.getBoolean("tc.ssl.trustAllCerts")) {
      // trust all certs
      trustManagers = new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
              //
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
              //
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }
          }
      };
    }

    sslCtxt.init(null, trustManagers, null);
    return sslCtxt;

  }

  @Override
  public boolean isUsingClientAuth() {
    return false;
  }
}
