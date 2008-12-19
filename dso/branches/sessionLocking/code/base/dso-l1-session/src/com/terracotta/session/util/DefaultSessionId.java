/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.SessionId;

public class DefaultSessionId implements SessionId {

  private final String        key;
  private final String        requestedId;
  private final String        externalId;
  private final Lock          lock;
  private final Lock          sessionInvalidatorLock;
  private final boolean       knownServerHop;
  private static final String SESSION_INVALIDATOR_LOCK_SUFFIX = ".session.invalidator";

  protected DefaultSessionId(final String internalKey, final String requestedId, final String externalId,
                             final int lockType, boolean knownServerHop) {
    Assert.pre(internalKey != null);
    Assert.pre(externalId != null);
    this.key = internalKey;
    this.requestedId = requestedId;
    this.externalId = externalId;
    this.knownServerHop = knownServerHop;
    this.lock = new Lock(this.key, lockType);
    this.sessionInvalidatorLock = new Lock(this.key + SESSION_INVALIDATOR_LOCK_SUFFIX, lockType);
  }

  public String getRequestedId() {
    return requestedId;
  }

  public String getKey() {
    return key;
  }

  public boolean isNew() {
    return requestedId == null;
  }

  public boolean isServerHop() {
    return !isNew() && knownServerHop;
  }

  public String toString() {
    return getClass().getName() + "{ " + "key=" + getKey() + ", requestedId=" + getRequestedId() + ", externalId="
           + getExternalId() + ", knownHop=" + knownServerHop + "}";
  }

  public String getExternalId() {
    return externalId;
  }

  public void commitLock() {
    lock.commitLock();
  }

  public void getWriteLock() {
    lock.getWriteLock();
  }

  public boolean tryWriteLock() {
    return lock.tryWriteLock();
  }

  public void commitSessionInvalidatorLock() {
    sessionInvalidatorLock.commitLock();
  }

  public void getSessionInvalidatorReadLock() {
    sessionInvalidatorLock.getReadLock();
  }

  public boolean trySessionInvalidatorWriteLock() {
    return sessionInvalidatorLock.tryWriteLock();
  }

}
