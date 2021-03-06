/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.terracottatech.config.PersistenceMode;

public class MapOfCollectionsTest extends GCTestBase {

  @Override
  protected Class getApplicationClass() {
    return MapOfCollectionsTestApp.class;
  }
  
  @Override
  protected void setupConfig(TestTVSConfigurationSetupManagerFactory configFactory) {
    this.gcConfigHelper.setGarbageCollectionInterval(20);
    super.setupConfig(configFactory);
    configFactory.setPersistenceMode(PersistenceMode.PERMANENT_STORE);
  }
}
