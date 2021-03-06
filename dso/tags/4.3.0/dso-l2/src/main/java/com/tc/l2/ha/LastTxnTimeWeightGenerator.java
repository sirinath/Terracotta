/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.l2.ha;

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.util.Assert;

import java.util.concurrent.atomic.AtomicLong;

public class LastTxnTimeWeightGenerator implements WeightGenerator, TransactionBatchListener {
  private final AtomicLong lastTxnTime = new AtomicLong(Long.MIN_VALUE);

  public LastTxnTimeWeightGenerator(TransactionBatchManager transactionBatchManager) {
    Assert.assertNotNull(transactionBatchManager);
    transactionBatchManager.registerForBatchTransaction(this);
  }

  /*
   * return (weight-generation-time - last-batch-transaction-time) return 0 if none txn yet.
   * negative weight, closest one win.
   */
  @Override
  public long getWeight() {
    long last = lastTxnTime.get();
    return (last == Long.MIN_VALUE) ? last : last - System.nanoTime();
  }

  @Override
  public void notifyTransactionBatchAdded(CommitTransactionMessage ctm) {
    lastTxnTime.set(System.nanoTime());
  }
}
