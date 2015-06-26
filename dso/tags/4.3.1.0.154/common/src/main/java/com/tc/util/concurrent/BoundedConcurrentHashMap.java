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
package com.tc.util.concurrent;

import java.util.concurrent.locks.Condition;

/**
 * This class works exactly in the same way as CHM with the exception that it is bounded on a segment basis. What this
 * means is that the "put" call will be blocked on reaching the limit for the segment until a "remove" call removes an
 * entry.
 */
public class BoundedConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
  private final int segmentSizeLimit;

  public BoundedConcurrentHashMap(long limit) {
    super();
    this.segmentSizeLimit = calculateSegmentSizeLimit(limit);
  }

  public BoundedConcurrentHashMap(int initialCapacity, long limit) {
    super(initialCapacity);
    this.segmentSizeLimit = calculateSegmentSizeLimit(limit);
  }

  public BoundedConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, long limit) {
    super(initialCapacity, loadFactor, concurrencyLevel);
    this.segmentSizeLimit = calculateSegmentSizeLimit(limit);
  }

  private int calculateSegmentSizeLimit(long limit) {
    long segmentSize = (int) Math.floor((double) limit / segments.length);
    segmentSize = segmentSize <= 0 ? 1 : segmentSize;
    segmentSize = segmentSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : segmentSize;
    return (int) segmentSize;
  }

  @Override
  protected Segment<K, V> createSegment(int cap, float loadFactor) {
    return new BoundedSegment<K, V>(cap, loadFactor);
  }

  private final class BoundedSegment<X, Y> extends Segment<X, Y> {
    private final Condition fullCondition;

    BoundedSegment(int initialCapacity, float lf) {
      super(initialCapacity, lf);
      fullCondition = this.newCondition();
    }

    @Override
    protected void prePut() {
      blockIfNecessary();
    }

    @Override
    protected void postRemove(HashEntry<X, Y> oldEntry) {
      int countBefore = this.count + 1;
      unblockIfNecessary(countBefore);
    }

    @Override
    protected void postClear(int countBefore) {
      unblockIfNecessary(countBefore);
    }

    private void blockIfNecessary() {
      boolean isInterrupted = false;
      try {
        while (count > segmentSizeLimit) {
          try {
            fullCondition.await();
          } catch (InterruptedException e) {
            isInterrupted = true;
          }
        }
      } finally {
        if (isInterrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void unblockIfNecessary(int countBefore) {
      if (countBefore > segmentSizeLimit) {
        fullCondition.signalAll();
      }
    }
  }
}
