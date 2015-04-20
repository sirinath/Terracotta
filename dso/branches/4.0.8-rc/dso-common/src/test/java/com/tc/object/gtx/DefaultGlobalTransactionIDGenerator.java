/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.object.tx.ServerTransactionID;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class DefaultGlobalTransactionIDGenerator implements GlobalTransactionIDGenerator {

  SortedSet gidSet  = new TreeSet();
  Map       sid2Gid = new HashMap();
  long      id      = 0;

  @Override
  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID) {

    GlobalTransactionID gid = (GlobalTransactionID) sid2Gid.get(serverTransactionID);
    if (gid == null) {
      gid = new GlobalTransactionID(id++);
      sid2Gid.put(serverTransactionID, gid);
      gidSet.add(gid);
    }
    return gid;
  }

  @Override
  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    if (gidSet.isEmpty()) {
      return GlobalTransactionID.NULL_ID;
    } else {
      GlobalTransactionID lowWaterMark = (GlobalTransactionID) gidSet.first();
      return lowWaterMark;
    }
  }
}
