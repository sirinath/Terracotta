/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.collections.map.ToolkitCacheImpl;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;

public class ToolkitCacheFactoryImpl extends AbstractPrimaryToolkitObjectFactory<ToolkitCacheImpl, ServerMap> {

  private ToolkitCacheFactoryImpl(ToolkitInternal toolkit, ToolkitFactoryInitializationContext context) {
    super(toolkit, context.getToolkitTypeRootsFactory().createAggregateDistributedTypeRoot(
        ToolkitTypeConstants.TOOLKIT_CACHE_ROOT_NAME, new ToolkitCacheDistributedTypeFactory(
        context.getSearchFactory(), context.getServerMapLocalStoreFactory()), context.getPlatformService()));
  }

  public static ToolkitCacheFactoryImpl newToolkitCacheFactory(ToolkitInternal toolkit,
                                                               ToolkitFactoryInitializationContext context) {
    return new ToolkitCacheFactoryImpl(toolkit, context);
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.CACHE;
  }

}
