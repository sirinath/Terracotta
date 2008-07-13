/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.JDOMException;

import com.google.inject.Provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class ModulesProvider implements Provider<CachedXmlModules> {

  public CachedXmlModules get() {
    try {
      InputStream data = new FileInputStream(new File("tuc-data.xml"));
      return new CachedXmlModules(data, "2.6.2");
    } catch (JDOMException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
