/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.util.Conversion;
import com.tc.util.OidLongArray;

import java.util.HashMap;
import java.util.Set;

public class OidBitsArrayMap {
  private static final TCLogger logger = TCLogging.getTestingLogger(OidBitsArrayMap.class);

  private final Database        oidDB;
  private final HashMap         map;
  private final int             bitsLength;
  private final int             longsPerDiskUnit;

  OidBitsArrayMap(int longsPerDiskUnit, Database oidDB) {
    this.oidDB = oidDB;
    this.longsPerDiskUnit = longsPerDiskUnit;
    this.bitsLength = longsPerDiskUnit * OidLongArray.BITS_PER_LONG;
    map = new HashMap();
  }

  public void clear() {
    map.clear();
  }

  public Long oidIndex(long oid) {
    return new Long(oid / bitsLength * bitsLength);
  }

  public OidLongArray getBitsArray(long oid) {
    Long mapIndex = oidIndex(oid);
    OidLongArray longAry = null;
    synchronized (map) {
      if (map.containsKey(mapIndex)) {
        longAry = (OidLongArray) map.get(mapIndex);
      }
    }
    return longAry;
  }

  private OidLongArray getOrLoadBitsArray(long oid) {
    Long mapIndex = oidIndex(oid);
    OidLongArray longAry;
    synchronized (map) {
      if (map.containsKey(mapIndex)) {
        longAry = (OidLongArray) map.get(mapIndex);
      } else {
        longAry = (oidDB != null) ? readDiskEntry(oid) : null;
        if (longAry == null) longAry = new OidLongArray(longsPerDiskUnit, mapIndex.longValue());
        map.put(mapIndex, longAry);
      }
    }
    return longAry;
  }

  private OidLongArray readDiskEntry(long oid) {
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    key.setData(Conversion.long2Bytes(oidIndex(oid)));
    try {
      OperationStatus status = oidDB.get(null, key, value, LockMode.DEFAULT);
      if (OperationStatus.SUCCESS.equals(status)) { return new OidLongArray(key.getData(), value.getData()); }
    } catch (DatabaseException e) {
      logger.error("Reading object ID " + oid + ":" + e);
    }
    return null;
  }

  private OidLongArray getAndModify(long oid, boolean doSet) {
    OidLongArray longAry = getOrLoadBitsArray(oid);
    int oidInArray = (int) (oid % bitsLength);
    synchronized (longAry) {
      if (doSet) {
        longAry.setBit(oidInArray);
      } else {
        longAry.clrBit(oidInArray);
      }
    }
    return (longAry);
  }

  public OidLongArray getAndSet(ObjectID id) {
    return (getAndModify(id.toLong(), true));
  }

  public OidLongArray getAndClr(ObjectID id) {
    return (getAndModify(id.toLong(), false));
  }

  public void removeIfEmpty(OidLongArray ary) {
    synchronized (map) {
      if (ary.isZero()) {
        map.remove(ary.getKey());
      }
    }
  }

  // for testing
  void loadBitsArrayFromDisk() {
    clear();
    Cursor cursor = null;
    try {
      cursor = oidDB.openCursor(null, CursorConfig.READ_COMMITTED);
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        OidLongArray bitsArray = new OidLongArray(key.getData(), value.getData());
        map.put(new Long(bitsArray.getKey()), bitsArray);
      }
      cursor.close();
      cursor = null;
    } catch (DatabaseException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (cursor != null) cursor.close();
      } catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  // for testing
  boolean contains(ObjectID id) {
    long oid = id.toLong();
    Long mapIndex = oidIndex(oid);
    synchronized (map) {
      if (map.containsKey(mapIndex)) {
        OidLongArray longAry = (OidLongArray) map.get(mapIndex);
        return (longAry.isSet((int) oid % bitsLength));
      }
    }
    return (false);
  }

  // for testing
  Set getMapKeySet() {
    return map.keySet();
  }
}
