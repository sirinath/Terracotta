/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;

public class LiteralAutoLocks {

  private LiteralAutoLocks() {
    //
  }

  /**
   * @return true if obj is an instance of a {@link com.tc.object.LiteralValues literal type} and is suitable for
   *         cluster-wide locking,
   */
  public static boolean isLiteralAutolock(final Object o) {
    if (o instanceof Manageable) { return false; }
    return (!(o instanceof Class)) && (!(o instanceof ObjectID)) && LiteralValues.isLiteralInstance(o);
  }

}
