/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.Clearable;
import com.tc.util.Assert;

import java.lang.ref.ReferenceQueue;

public class TCObjectLogical extends TCObjectImpl {

  public TCObjectLogical(ReferenceQueue queue, ObjectID id, Object peer, TCClass tcc) {
    super(queue, id, peer, tcc);
  }

  public void logicalInvoke(int method, String methodName, Object[] parameters) {
    this.markAccessed();
    getObjectManager().getTransactionManager().logicalInvoke(this, method, methodName, parameters);
  }

  protected boolean isEvictable() {
    if (getPeerObject() instanceof Clearable) {
      return true;
    } else {
      return false;
    }
  }
  
  protected int clearReferences(Object pojo, int toClear) {
    Assert.assertTrue(pojo instanceof Clearable);
    Clearable clearable = (Clearable) pojo;
    return clearable.__tc_clearReferences(toClear);
  }

}
