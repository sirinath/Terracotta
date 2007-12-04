/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;
import java.io.Serializable;

public class LockStats implements TCSerializable, Serializable {
  private final static int NON_SET_TIME_MILLIS = -1;

  private long             numOfPendingRequests;
  private long             numOfPendingWaiters;
  private long             numOfLockRequested;
  private long             numOfLockHopRequests;
  private long             totalWaitTimeToAwardedInMillis;
  private long             numOfLockAwarded;
  private long             totalRecordedHeldTimeInMillis;
  private long             totalRecordedReleases;
  private long             avgHeldTimeInMillis;
  private long             avgWaitTimeToAwardInMillis;
  private long             totalNestedDepth;
  private long             avgNestedDepth;

  public LockStats() {
    this.avgHeldTimeInMillis = NON_SET_TIME_MILLIS;
    this.avgWaitTimeToAwardInMillis = NON_SET_TIME_MILLIS;
  }

  public void aggregateStatistics(long pendingRequests, long pendingWaiters, long lockRequested, long lockHopRequests,
                                  long lockAwarded, long timeToAwardedInMillis, long heldTimeInMillis,
                                  long numOfReleases) {
    this.numOfPendingRequests += pendingRequests;
    this.numOfPendingWaiters += pendingWaiters;
    this.numOfLockRequested += lockRequested;
    this.numOfLockHopRequests += lockHopRequests;
    this.numOfLockAwarded += lockAwarded;
    this.totalWaitTimeToAwardedInMillis += timeToAwardedInMillis;
    this.totalRecordedHeldTimeInMillis += heldTimeInMillis;
    this.totalRecordedReleases += numOfReleases;
    this.avgHeldTimeInMillis = getAvgHeldTimeInMillis();
    this.avgWaitTimeToAwardInMillis = getAvgWaitTimeToAwardInMillis();
  }

  public void recordLockRequested() {
    this.numOfLockRequested++;
    this.numOfPendingRequests++;
  }

  public void recordLockHopRequested() {
    this.numOfLockHopRequests++;
  }

  public void recordLockAwarded(long waitTimeInMillis, int nestedLockDepth) {
    this.numOfLockAwarded++;
    this.numOfPendingRequests--;
    this.totalWaitTimeToAwardedInMillis += waitTimeInMillis;
    this.totalNestedDepth += nestedLockDepth;
    getAvgWaitTimeToAwardInMillis();
  }

  public void aggregateLockWaitTime(long waitTimeInMillis) {
    this.totalWaitTimeToAwardedInMillis += waitTimeInMillis;
  }

  public void recordLockRejected() {
    this.numOfPendingRequests--;
  }

  public void recordLockReleased(long heldTimeInMillis) {
    this.totalRecordedHeldTimeInMillis += heldTimeInMillis;
    this.totalRecordedReleases++;
    getAvgHeldTimeInMillis();
  }

  public long getNumOfLockAwarded() {
    return numOfLockAwarded;
  }

  public long getNumOfLockHopRequests() {
    return numOfLockHopRequests;
  }

  public long getNumOfLockRequested() {
    return numOfLockRequested;
  }

  public long getNumOfPendingWaiters() {
    return numOfPendingWaiters;
  }

  public long getNumOfLockReleased() {
    return totalRecordedReleases;
  }

  public long getNumOfLockPendingRequested() {
    return numOfPendingRequests;
  }

  public long getTotalRecordedHeldTimeInMillis() {
    return totalRecordedHeldTimeInMillis;
  }

  public long getTotalRecordedReleases() {
    return totalRecordedReleases;
  }

  public long getTotalWaitTimeToAwardedInMillis() {
    return totalWaitTimeToAwardedInMillis;
  }

  public void aggregateAvgHeldTimeInMillis(long totalHeldTimeInMillis, long numOfReleases) {
    avgHeldTimeInMillis = NON_SET_TIME_MILLIS;
    numOfReleases += this.totalRecordedReleases;
    totalHeldTimeInMillis += this.totalRecordedHeldTimeInMillis;
    if (numOfReleases > 0) {
      avgHeldTimeInMillis = totalHeldTimeInMillis / numOfReleases;
    }
  }

  public long getAvgHeldTimeInMillis() {
    // if (avgHeldTimeInMillis == NON_SET_TIME_MILLIS) {
    aggregateAvgHeldTimeInMillis(0, 0);
    // }
    return avgHeldTimeInMillis;
  }

  public long getAvgWaitTimeToAwardInMillis() {
    // if (avgWaitTimeToAwardInMillis == NON_SET_TIME_MILLIS) {
    aggregateAvgWaitTimeInMillis(0, 0);
    // }
    return avgWaitTimeToAwardInMillis;
  }
  
  public long getAvgNestedLockDepth() {
    if (numOfLockAwarded == 0) { return 0; }
    return totalNestedDepth/numOfLockAwarded;
  }

  public void aggregateAvgWaitTimeInMillis(long totalWaitTimeInMillis, long numOfAwarded) {
    avgWaitTimeToAwardInMillis = NON_SET_TIME_MILLIS;
    numOfAwarded += this.numOfLockAwarded;
    totalWaitTimeInMillis += this.totalWaitTimeToAwardedInMillis;
    if (numOfAwarded > 0) {
      avgWaitTimeToAwardInMillis = totalWaitTimeInMillis / numOfAwarded;
    }
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    numOfPendingRequests = serialInput.readLong();
    numOfPendingWaiters = serialInput.readLong();
    numOfLockRequested = serialInput.readLong();
    numOfLockHopRequests = serialInput.readLong();
    totalWaitTimeToAwardedInMillis = serialInput.readLong();
    numOfLockAwarded = serialInput.readLong();
    totalRecordedHeldTimeInMillis = serialInput.readLong();
    totalRecordedReleases = serialInput.readLong();
    avgHeldTimeInMillis = serialInput.readLong();
    avgWaitTimeToAwardInMillis = serialInput.readLong();
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(numOfPendingRequests);
    serialOutput.writeLong(numOfPendingWaiters);
    serialOutput.writeLong(numOfLockRequested);
    serialOutput.writeLong(numOfLockHopRequests);
    serialOutput.writeLong(totalWaitTimeToAwardedInMillis);
    serialOutput.writeLong(numOfLockAwarded);
    serialOutput.writeLong(totalRecordedHeldTimeInMillis);
    serialOutput.writeLong(totalRecordedReleases);
    serialOutput.writeLong(avgHeldTimeInMillis);
    serialOutput.writeLong(avgWaitTimeToAwardInMillis);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("numOfPendingRequests: ");
    sb.append(numOfPendingRequests);
    sb.append(", numOfPendingWaiters: ");
    sb.append(numOfPendingWaiters);
    sb.append(", numOfLockRequested: ");
    sb.append(numOfLockRequested);
    sb.append(", numOfLockHopRequests: ");
    sb.append(numOfLockHopRequests);
    sb.append(", numOfLockAwarded: ");
    sb.append(numOfLockAwarded);
    sb.append(", totalRecordedReleases: ");
    sb.append(totalRecordedReleases);
    sb.append(", avgHeldTimeInMillis: ");
    sb.append(avgHeldTimeInMillis);
    sb.append(", avgWaitTimeToAwardInMillis: ");
    sb.append(avgWaitTimeToAwardInMillis);
    return sb.toString();
  }
}
