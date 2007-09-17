/**
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

public class Jdk15PreInstrumentedConfiguration
      extends TerracottaConfiguratorModule {

   protected void addInstrumentation(final BundleContext context) {
      super.addInstrumentation(context);
   }

}
