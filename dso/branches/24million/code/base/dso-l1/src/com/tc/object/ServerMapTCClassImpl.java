/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.field.TCFieldFactory;
import com.tc.object.loaders.LoaderDescription;

public class ServerMapTCClassImpl extends TCClassImpl implements TCClass {

  private final RemoteServerMapManager serverMapManager;

  ServerMapTCClassImpl(final RemoteServerMapManager serverMapManager, final TCFieldFactory factory,
                       final TCClassFactory clazzFactory, final ClientObjectManager objectManager, final Class peer,
                       final Class logicalSuperClass, final LoaderDescription loaderDesc,
                       final String logicalExtendingClassName, final boolean isLogical,
                       final boolean isCallConstructor, final boolean onLoadInjection, final String onLoadScript,
                       final String onLoadMethod, final boolean useNonDefaultConstructor,
                       final boolean useResolveLockWhileClearing, final String postCreateMethod,
                       final String preCreateMethod) {
    super(factory, clazzFactory, objectManager, peer, logicalSuperClass, loaderDesc, logicalExtendingClassName,
          isLogical, isCallConstructor, onLoadInjection, onLoadScript, onLoadMethod, useNonDefaultConstructor,
          useResolveLockWhileClearing, postCreateMethod, preCreateMethod);
    this.serverMapManager = serverMapManager;
  }

  @Override
  public TCObject createTCObject(final ObjectID id, final Object pojo, final boolean isNew) {
    //System.out.println("POJO:!!!" + pojo);
    if (pojo != null && !pojo.getClass().getName().equals(TCClassFactory.CDSM_DSO_CLASSNAME)) {
      // bad formatter
      throw new AssertionError("This class should be used only for " + TCClassFactory.CDSM_DSO_CLASSNAME
                               + " but pojo : " + pojo.getClass().getName());
    }
    return new TCObjectServerMap(getObjectManager(), this.serverMapManager, id, pojo, this, isNew);
  }

}
