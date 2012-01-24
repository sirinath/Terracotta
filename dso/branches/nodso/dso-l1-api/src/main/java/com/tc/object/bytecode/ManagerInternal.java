/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.net.GroupID;
import com.tc.object.TCObject;
import com.tc.object.locks.LockID;
import com.tc.object.locks.Notify;
import com.tc.object.locks.TerracottaLockingInternal;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.NVPair;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.search.SearchQueryResults;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public interface ManagerInternal extends Manager, TerracottaLockingInternal {

  MetaDataDescriptor createMetaDataDescriptor(String category);

  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize);

  public NVPair createNVPair(String name, Object value);

  void verifyCapability(String capability);

  void fireOperatorEvent(EventType eventLevel, EventSubsystem subsystem, String eventMessage);

  void stopImmediate();

  void initForTests(CountDownLatch latch);

  void lockIDWait(final LockID lock, final long timeout) throws InterruptedException;

  Notify lockIDNotifyAll(final LockID lock);

  Notify lockIDNotify(final LockID lock);

  Object lookupOrCreateRoot(final String name, final Object object, final GroupID gid);

  TCObject lookupOrCreate(final Object obj, final Object parentObject);

  GroupID[] getGroupIDs();
}
