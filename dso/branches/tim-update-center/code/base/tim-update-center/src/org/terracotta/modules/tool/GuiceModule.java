/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import org.terracotta.modules.tool.commands.CommandRegistry;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

/**
 * Module definition for Guice dependency injection.
 */
public class GuiceModule implements Module {

  public void configure(Binder binder) {
    binder.bind(Modules.class).toProvider(ModulesProvider.class).in(Scopes.SINGLETON);
    binder.bind(CommandRegistry.class).in(Scopes.SINGLETON);
  }

}
