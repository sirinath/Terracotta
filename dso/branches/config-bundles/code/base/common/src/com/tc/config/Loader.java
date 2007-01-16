/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.schema.migrate.V1toV3;
import com.terracottatech.configV3.TcConfigDocument;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Responsible for creating a TcConfigDocument of the current version from any published configuration schema version.
 */

public class Loader {

  private V1toV3 v1toV3Converter;

  public Loader() {
    v1toV3Converter = new V1toV3();
  }

  public TcConfigDocument parse(File file) throws IOException, XmlException {
    return v1toV3Converter.parse(file);
  }

  public TcConfigDocument parse(File file, XmlOptions xmlOptions) throws IOException, XmlException {
    return v1toV3Converter.parse(file, xmlOptions);
  }

  public TcConfigDocument parse(String xmlText) throws IOException, XmlException {
    return v1toV3Converter.parse(xmlText);
  }

  public TcConfigDocument parse(String xmlText, XmlOptions xmlOptions) throws XmlException {
    return v1toV3Converter.parse(xmlText, xmlOptions);
  }

  public TcConfigDocument parse(InputStream stream) throws IOException, XmlException {
    return v1toV3Converter.parse(stream);
  }

  public TcConfigDocument parse(InputStream stream, XmlOptions xmlOptions) throws IOException, XmlException {
    return v1toV3Converter.parse(stream, xmlOptions);
  }

  public TcConfigDocument parse(URL url) throws IOException, XmlException {
    return v1toV3Converter.parse(url);
  }

  public TcConfigDocument parse(URL url, XmlOptions xmlOptions) throws IOException, XmlException {
    return v1toV3Converter.parse(url, xmlOptions);
  }

  public boolean testIsOld(File file) throws IOException, XmlException {
    try {
      return v1toV3Converter.testIsV1(file);
    } catch (XmlException xmle) {
      return false;
    }
  }

  public boolean testIsCurrent(File file) throws IOException, XmlException {
    try {
      v1toV3Converter.testIsV3(file);
      return true;
    } catch (XmlException xmle) {
      return false;
    }
  }

  public boolean updateToCurrent(File file) throws IOException, XmlException {
    try {
      v1toV3Converter.update(file);
      return true;
    } catch (XmlException xmle) {
      return false;
    }
  }
}
