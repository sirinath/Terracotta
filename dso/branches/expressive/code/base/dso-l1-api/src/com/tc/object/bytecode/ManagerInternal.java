/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.TCObject;

/**
 * Methods defined here can freely reference internal (non-API) types (eg. TCObject). To access methods here you want to
 * use ManagerInternalUtil (as opposed to ManagerUtil)
 */
public interface ManagerInternal extends Manager {

  TCObject lookupExistingOrNull(Object obj);

  TCObject lookupArrayTCObjectOrNull(Object array);

  void charArrayCopy(char[] src, int srcPos, char[] dest, int destPos, int length, TCObject tco);

}
