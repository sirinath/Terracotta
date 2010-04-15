/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectExternal;

public interface ObjectLookup {

  TCObjectExternal lookupExistingOrNull(Object pojo);

  boolean isPortableInstance(Object element);

  Object lookupObject(ObjectID oid) throws ClassNotFoundException;

}
