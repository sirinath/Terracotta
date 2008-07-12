/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.JDOMException;
import org.terracotta.modules.tool.commands.CommandRegistry;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;

import java.io.File;
import java.io.IOException;

/**
 * Module definition for Guice dependency injection.
 */
public class GuiceModule implements Module {

  public void configure(Binder binder) {
    binder.bind(Modules.class).toProvider(new Provider<CachedXmlModules>() {
      public CachedXmlModules get() {
        try {
          return new CachedXmlModules(new File("tuc-modules.xml"));
        } catch (JDOMException e) {
          throw new RuntimeException(e);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
    //binder.bind(Modules.class).to(CachedXmlModules.class).in(Scopes.SINGLETON);
    binder.bind(CommandRegistry.class).in(Scopes.SINGLETON);
  }

}
