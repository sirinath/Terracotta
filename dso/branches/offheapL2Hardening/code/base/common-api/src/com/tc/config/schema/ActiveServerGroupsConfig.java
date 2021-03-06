/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

public interface ActiveServerGroupsConfig extends Config {
	ActiveServerGroupConfig[] getActiveServerGroupArray();

	ActiveServerGroupConfig getActiveServerGroupForL2(String l2Name);

	int getActiveServerGroupCount();
}
