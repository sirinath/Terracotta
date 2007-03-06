/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.net.groups.AbstractGroupMessage;

public class ObjectSyncMessageFactory {

  public static AbstractGroupMessage createObjectSyncMessageFrom(ManagedObjectSyncContext mosc) {
    ObjectSyncMessage msg = new ObjectSyncMessage(ObjectSyncMessage.MANAGED_OBJECT_SYNC_TYPE);
    msg.initialize(mosc.getOIDs(), mosc.getDNACount(), mosc.getSerializedDNAs(), mosc.getObjectSerializer(), mosc.getRootsMap());
    return msg;
  }

}
