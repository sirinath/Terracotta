/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author Ludovic Orban
 */
public class ServerURL {

  private final URL theURL;
  private final int timeout;

  public ServerURL(String protocol, String host, int port, String file) throws MalformedURLException {
    this(protocol, host, port, file, -1);
  }

  public ServerURL(String protocol, String host, int port, String file, int timeout) throws MalformedURLException {
    this.timeout = timeout;
    this.theURL = new URL(protocol, host, port, file);
  }

  public InputStream openStream() throws IOException {
    URLConnection urlConnection;

    if ("https".equals(theURL.getProtocol())) {
      HttpsURLConnection sslUrlConnection = (HttpsURLConnection)theURL.openConnection();

      // don't verify hostname
      sslUrlConnection.setHostnameVerifier(new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      });

      // trust all certs
      TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
            public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
            }

            public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }
          }
      };

      try {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, null);
        sslUrlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
      } catch (Exception e) {
        throw new RuntimeException("unable to create SSL connection from " + theURL, e);
      }

      urlConnection = sslUrlConnection;
    } else {
      urlConnection = theURL.openConnection();
    }

    if (timeout > -1) {
      urlConnection.setConnectTimeout(timeout);
      urlConnection.setReadTimeout(timeout);
    }

    return urlConnection.getInputStream();
  }

  @Override
  public String toString() {
    return theURL.toString();
  }
}
