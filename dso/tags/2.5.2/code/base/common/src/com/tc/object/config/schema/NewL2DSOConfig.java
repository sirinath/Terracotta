/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.NewConfig;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;

/**
 * Represents all configuration read by the DSO L2 and which is independent of application.
 */
public interface NewL2DSOConfig extends NewConfig {

  ConfigItem persistenceMode();

  BooleanConfigItem garbageCollectionEnabled();

  BooleanConfigItem garbageCollectionVerbose();

  IntConfigItem garbageCollectionInterval();

  IntConfigItem listenPort();

  IntConfigItem l2GroupPort();

  StringConfigItem host();

  IntConfigItem clientReconnectWindow();

  StringConfigItem bind();
}
