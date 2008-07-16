/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.JDOMException;

import com.google.inject.Provider;
import com.tc.util.ProductInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

class ModulesProvider implements Provider<CachedModules> {

  public CachedModules get() {
    try {
      String tcversion = System.getProperty("org.terracotta.modules.tool.tuc.tc-version");
      if (tcversion == null) tcversion = ProductInfo.getInstance().version();
      String manifestUrl = System.getProperty("org.terracotta.modules.tool.tuc.manifest-url");
      InputStream data;
      if (manifestUrl == null) {
        data = CachedModules.class.getResourceAsStream("/tuc-manifest.xml");
      } else {
        URL url = new URL(manifestUrl);
        data = new FileInputStream(new File(url.toURI()));
      }
      return new CachedModules(data, tcversion);
    } catch (JDOMException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
