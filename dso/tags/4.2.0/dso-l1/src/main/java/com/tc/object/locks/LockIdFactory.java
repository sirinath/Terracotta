/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Manager;

public class LockIdFactory {

  private final Manager mgr;

  public LockIdFactory(final Manager mgr) {
    this.mgr = mgr;
  }

  public LockID generateLockIdentifier(final Object obj) {
    if (obj instanceof Long) {
      return generateLockIdentifier(((Long) obj).longValue());
    } else if (obj instanceof String) {
      return generateLockIdentifier((String) obj);
    } else {
      final TCObject tco = this.mgr.lookupExistingOrNull(obj);
      if (tco != null) {
        if (tco.autoLockingDisabled()) {
          return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
        } else {
          return new DsoLockID(tco.getObjectID());
        }
      } else if (this.mgr.isLiteralAutolock(obj)) {
        try {
          return new DsoLiteralLockID(obj);
        } catch (final IllegalArgumentException e) {
          return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
        }
      } else {
        return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
      }
    }
  }

  public LockID generateLockIdentifier(final Object obj, final String fieldName) {
    TCObject tco;
    if (obj instanceof TCObject) {
      tco = (TCObject) obj;
    } else {
      tco = this.mgr.lookupExistingOrNull(obj);
    }

    if ((tco == null) || tco.autoLockingDisabled()) {
      return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
    } else {
      return new DsoVolatileLockID(tco.getObjectID(), fieldName);
    }
  }

  public LockID generateLockIdentifier(final long l) {
    return new LongLockID(l);
  }

  public LockID generateLockIdentifier(final String str) {
    return new StringLockID(str);
  }
}
