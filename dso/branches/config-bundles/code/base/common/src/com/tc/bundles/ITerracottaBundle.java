/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import com.tc.bundles.instrumentation.InstrumentationSpecification;

/**
 * Represents the first version of the "Terracotta" bundle interface; this will go away when we OSGi-ize things.
 */
public interface ITerracottaBundle {

  InstrumentationSpecification getInstrumentationSpecification();

}
