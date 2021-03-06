/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.NewHaConfig;
import com.tc.config.schema.NewSystemConfig;
import com.tc.config.schema.UpdateCheckConfig;
import com.tc.object.config.schema.NewDSOApplicationConfig;
import com.tc.object.config.schema.NewL2DSOConfig;

import java.io.InputStream;

/**
 * Knows how to set up configuration for L2.
 */
public interface L2TVSConfigurationSetupManager {
  NewCommonL2Config commonl2Config();

  NewSystemConfig systemConfig();

  NewL2DSOConfig dsoL2Config();

  NewHaConfig haConfig();

  UpdateCheckConfig updateCheckConfig();

  ActiveServerGroupsConfig activeServerGroupsConfig();
  
  ActiveServerGroupConfig getActiveServerGroupForThisL2();

  String[] applicationNames();

  NewDSOApplicationConfig dsoApplicationConfigFor(String applicationName);

  String describeSources();

  InputStream rawConfigFile();

  InputStream effectiveConfigFile();

  String[] allCurrentlyKnownServers();
  
  String getL2Identifier();

  NewCommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException;

  NewL2DSOConfig dsoL2ConfigFor(String name) throws ConfigurationSetupException;
}
