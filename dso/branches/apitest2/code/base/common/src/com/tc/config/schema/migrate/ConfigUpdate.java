/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.migrate;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import java.io.IOException;
import java.io.InputStream;

public interface ConfigUpdate {
  
  InputStream convert(InputStream in, XmlOptions xmlOptions) throws XmlException, IOException;
  
  XmlOptions createDefaultXmlOptions();
}
