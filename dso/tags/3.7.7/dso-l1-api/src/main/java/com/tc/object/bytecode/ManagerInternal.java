/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.ObjectID;
import com.tc.object.locks.TerracottaLockingInternal;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.search.SearchQueryResults;
import com.terracottatech.search.NVPair;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public interface ManagerInternal extends Manager, TerracottaLockingInternal {

  MetaDataDescriptor createMetaDataDescriptor(String category);

  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttribues, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn);

  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn);

  public NVPair createNVPair(String name, Object value);

  void verifyCapability(String capability);

  void fireOperatorEvent(EventType eventLevel, EventSubsystem subsystem, String eventMessage);

  void stopImmediate();

  void initForTests(CountDownLatch latch);

  void addTransactionCompleteListener(TransactionCompleteListener listener);

  void skipBroadcastForCurrentTransaction(ObjectID objectID);

}
