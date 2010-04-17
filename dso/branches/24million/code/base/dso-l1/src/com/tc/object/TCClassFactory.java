/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.object.applicator.ChangeApplicator;

public interface TCClassFactory {

  // Ugly Hardcoding since the class is elsewhere
  public static final String CDSM_DSO_CLASSNAME = "org.terracotta.collections.ConcurrentDistributedServerMapDso";

  public TCClass getOrCreate(Class clazz, ClientObjectManager objectManager);

  public ChangeApplicator createApplicatorFor(TCClass clazz, boolean indexed);

}
