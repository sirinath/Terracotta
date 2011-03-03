/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package java.util;

import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;

import java.util.TreeMap.Entry;

public class TreeMapEntryWrapper extends Entry {

  TreeMapEntryWrapper(Object key, Object value, Entry parent) {
    super(key, value, parent);
  }

  public synchronized Object getValue() {
    Object o = super.getValue();
    if (o instanceof ObjectID) {
      o = ManagerUtil.lookupObject((ObjectID) o);
      super.setValue(o);
    }
    return o;
  }

  public synchronized Object setValue(Object value) {
    return super.setValue(value);
  }

  public synchronized boolean clear() {
    Object o = super.getValue();
    if (o instanceof ObjectID) { return false; }

    if (o instanceof Manageable) {
      super.setValue(((Manageable) o).__tc_managed().getObjectID());
      return true;
    }

    return false;
  }

  public static int clearReferences(Set entrySet, int toClear) {
    int cleared = 0;

    for (Iterator i = entrySet.iterator(); i.hasNext();) {
      TreeMapEntryWrapper entry = (TreeMapEntryWrapper) i.next();
      if (entry.clear()) {
        cleared++;
      }
      if (cleared == toClear) break;
    }

    return cleared;
  }

}
