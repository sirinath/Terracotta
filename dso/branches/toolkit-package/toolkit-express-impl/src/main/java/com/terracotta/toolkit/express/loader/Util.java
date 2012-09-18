/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

public class Util {

  public static byte[] extract(final InputStream in) throws IOException {
    if (in == null) { throw new NullPointerException(); }

    try {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final byte[] data = new byte[4096];
      int read = 0;
      while ((read = in.read(data, 0, data.length)) > 0) {
        out.write(data, 0, read);
      }
      return out.toByteArray();
    } finally {
      closeQuietly(in);
    }
  }

  public static int getNumJarSeparators(final String str) {
    int rv = 0;
    final int length = str.length();
    for (int i = 0; i < length; i++) {
      final char ch = str.charAt(i);
      if (ch == '!' && i < length - 1 && str.charAt(i + 1) == '/') {
        rv++;
      }
    }
    return rv;
  }

  public static boolean isDirectoryUrl(URL url) {
    File file = toFile(url);
    if (file != null && file.isDirectory()) return true;
    return false;
  }

  public static void closeQuietly(final InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  public static void closeQuietly(final OutputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  public static int copy(final InputStream in, final OutputStream out) throws IOException {
    byte[] buffer = new byte[1024 * 4];
    int count = 0;
    int n = 0;
    while (-1 != (n = in.read(buffer))) {
      out.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  public static void copyFile(final File srcFile, final File destFile) throws IOException {
    FileInputStream input = new FileInputStream(srcFile);
    try {
      FileOutputStream output = new FileOutputStream(destFile);
      try {
        copy(input, output);
      } finally {
        closeQuietly(output);
      }
    } finally {
      closeQuietly(input);
    }
  }

  public static URL toURL(final File file) throws MalformedURLException {
    return file.toURI().toURL();
  }

  public static File toFile(URL url) {
    if (!url.toExternalForm().startsWith("file")) { return null; }
    String path = url.getPath();
    try {
      return new File(URLDecoder.decode(path, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }
}
