/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.migrate;

import org.apache.xmlbeans.XmlOptions;

import java.io.InputStream;

/**
 * No new mandatory elements. No deprecated elements.
 */
public class V2toV3 extends BaseConfigUpdate {

  public InputStream convert(InputStream in, XmlOptions xmlOptions) {
    return in;
  }
}
