/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.Manager;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;

public class ServerMapTCClassImpl extends TCClassImpl implements TCClass {

  private final L1ServerMapLocalCacheManager globalLocalCacheManager;
  private final RemoteServerMapManager       remoteServerMapManager;
  private final Manager                      manager;

  ServerMapTCClassImpl(final Manager manager, final L1ServerMapLocalCacheManager globalLocalCacheManager,
                       final RemoteServerMapManager remoteServerMapManager, final TCFieldFactory factory,
                       final TCClassFactory clazzFactory, final ClientObjectManager objectManager, final Class peer,
                       final Class logicalSuperClass, final String logicalExtendingClassName, final boolean isLogical,
                       final boolean useNonDefaultConstructor, final boolean useResolveLockWhileClearing,
                       final String postCreateMethod, final String preCreateMethod) {
    super(factory, clazzFactory, objectManager, peer, logicalSuperClass, logicalExtendingClassName, isLogical,
          useNonDefaultConstructor, useResolveLockWhileClearing, postCreateMethod, preCreateMethod);
    this.globalLocalCacheManager = globalLocalCacheManager;
    this.remoteServerMapManager = remoteServerMapManager;
    this.manager = manager;
  }

  @Override
  public TCObject createTCObject(final ObjectID id, final Object pojo, final boolean isNew) {
    if (pojo != null && !(pojo.getClass().getName().equals(TCClassFactory.SERVER_MAP_CLASSNAME))) {
      // bad formatter
      throw new AssertionError("This class should be used only for " + TCClassFactory.SERVER_MAP_CLASSNAME
                               + " but pojo : " + pojo.getClass().getName());
    }
    return new TCObjectServerMapImpl(this.manager, getObjectManager(), this.remoteServerMapManager, id, pojo, this,
                                     isNew, this.globalLocalCacheManager);
  }

}
