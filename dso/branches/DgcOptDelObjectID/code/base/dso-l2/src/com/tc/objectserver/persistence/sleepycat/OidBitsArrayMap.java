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
import com.sleepycat.je.Transaction;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.util.Conversion;
import com.tc.util.OidLongArray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class OidBitsArrayMap {
  private static final TCLogger logger = TCLogging.getTestingLogger(OidBitsArrayMap.class);

  private final Database        oidDB;
  private final HashMap         map;
  private final int             bitsLength;
  private final int             longsPerDiskUnit;
  private final int             auxKey;

  OidBitsArrayMap(int longsPerDiskUnit, Database oidDB) {
    this(longsPerDiskUnit, oidDB, 0);
  }

  OidBitsArrayMap(int longsPerDiskUnit, Database oidDB, int auxKey) {
    this.oidDB = oidDB;
    this.longsPerDiskUnit = longsPerDiskUnit;
    this.bitsLength = longsPerDiskUnit * OidLongArray.BITS_PER_LONG;
    map = new HashMap();
    this.auxKey = auxKey;
  }

  public void clear() {
    map.clear();
  }

  public int getAuxKey() {
    return auxKey;
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
        longAry = null;
        if (oidDB != null) {
          try {
            longAry = readDiskEntry(null, oid);
          } catch (DatabaseException e) {
            logger.error("Reading object ID " + oid + ":" + e);
          }
        }
        if (longAry == null) longAry = new OidLongArray(longsPerDiskUnit, mapIndex.longValue());
        map.put(mapIndex, longAry);
      }
    }
    return longAry;
  }

  public OidLongArray readDiskEntry(Transaction txn, long oid) throws DatabaseException {
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    long aryIndex = oidIndex(oid);
    key.setData(Conversion.long2Bytes(aryIndex + auxKey));
    OperationStatus status = oidDB.get(txn, key, value, LockMode.DEFAULT);
    if (OperationStatus.SUCCESS.equals(status)) { return new OidLongArray(aryIndex, value.getData()); }
    return null;
  }

  public void writeDiskEntry(Transaction txn, OidLongArray bits) throws DatabaseException {
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    key.setData(bits.keyToBytes(auxKey));

    if (!bits.isZero()) {
      value.setData(bits.arrayToBytes());
      if (!OperationStatus.SUCCESS.equals(this.oidDB.put(txn, key, value))) {
        //
        throw new DatabaseException("Failed to update oidDB at " + bits.getKey());
      }
    } else {
      OperationStatus status = this.oidDB.delete(txn, key);
      // OperationStatus.NOTFOUND happened if added and then deleted in the same batch
      if (!OperationStatus.SUCCESS.equals(status) && !OperationStatus.NOTFOUND.equals(status)) {
        //
        throw new DatabaseException("Failed to delete oidDB at " + bits.getKey());
      }
      removeIfEmpty(bits);
    }

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

  public boolean contains(ObjectID id) {
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
  void loadAllFromDisk() {
    synchronized (map) {
      clear();
      Cursor cursor = null;
      try {
        cursor = oidDB.openCursor(null, CursorConfig.READ_COMMITTED);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
          // load its only records indicated by auxKey
          long index = Conversion.bytes2Long(key.getData());
          if (index == (oidIndex(index) + auxKey)) {
            index -= auxKey;
            OidLongArray bitsArray = new OidLongArray(index, value.getData());
            map.put(new Long(index), bitsArray);
          }
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
  }

  // for testing
  void saveAllToDisk() {
    synchronized (map) {
      // use another set to avoid ConcurrentModificationException
      Set dupKeySet = new HashSet();
      dupKeySet.addAll(map.keySet());
      Iterator i = dupKeySet.iterator();
      while (i.hasNext()) {
        OidLongArray bitsArray = (OidLongArray) map.get(i.next());
        try {
          writeDiskEntry(null, bitsArray);
        } catch (DatabaseException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  // for testing
  Set getMapKeySet() {
    return map.keySet();
  }
}
