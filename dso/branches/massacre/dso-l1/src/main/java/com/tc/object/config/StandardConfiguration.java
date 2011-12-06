/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.bundles.LegacyDefaultModuleBase;
import com.tc.object.bytecode.DSOUnsafeAdapter;
import com.tc.object.bytecode.UnsafeAdapter;

public class StandardConfiguration extends LegacyDefaultModuleBase {

  public StandardConfiguration(StandardDSOClientConfigHelper configHelper) {
    super(configHelper);
  }

  @Override
  public void apply() {
    configArrayTypes();
    configUnsafe();
    configThirdParty();
  }

  private void configUnsafe() {
    TransparencyClassSpec spec;

    spec = getOrCreateSpec("sun.misc.Unsafe");
    spec.setCustomClassAdapter(new UnsafeAdapter());
    spec.markPreInstrumented();

    spec = getOrCreateSpec("com.tcclient.util.DSOUnsafe");
    spec.setCustomClassAdapter(new DSOUnsafeAdapter());
    spec.markPreInstrumented();
  }

  private void configArrayTypes() {
    final TransparencyClassSpec spec = getOrCreateSpec("java.util.Arrays");
    spec.addDoNotInstrument("copyOfRange");
    spec.addDoNotInstrument("copyOf");
    getOrCreateSpec("java.util.Arrays$ArrayList");
  }

  private void configThirdParty() {
    configHelper.addIncludePattern("gnu.trove..*", false, false, true);
  }

}
