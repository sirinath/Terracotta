/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.msg.LockRequestMessageConsts;

/**
 * Encapsulates an invocation of Object.wait(...)
 */
public final class WaitInvocation {

  private final Signature signature;

  private long            millis;
  private int             nanos;
  private long            mark = LockRequestMessageConsts.UNITIALIZED_WAIT_TIME;

  /**
   * Untimed wait
   */
  public WaitInvocation() {
    this(NO_ARGS, LockRequestMessageConsts.UNITIALIZED_WAIT_TIME, LockRequestMessageConsts.UNITIALIZED_WAIT_TIME);
  }

  /**
   * Wait for millis
   * @param millis Milliseconds to wait
   */
  public WaitInvocation(long millis) {
    this(LONG, millis, LockRequestMessageConsts.UNITIALIZED_WAIT_TIME);
  }

  /**
   * Wait for millis and nanos
   * @param millis Milliseconds
   * @param nanos Nanoseconds
   */
  public WaitInvocation(long millis, int nanos) {
    this(LONG_INT, millis, nanos);
  }

  /**
   * Wait on method signature 
   * @param signature Method signature
   * @param millis Milliseconds
   * @param nanos Nanoseconds
   */
  private WaitInvocation(Signature signature, long millis, int nanos) {
    this.signature = signature;

    if (signature == LONG) {
      if (millis < 0) { throw new IllegalArgumentException("Invalid milliseconds argument to wait(long): " + millis); }
    } else if (signature == LONG_INT) {
      if (millis < 0) { throw new IllegalArgumentException("Invalid milliseconds argument to wait(long, int): "
                                                           + millis); }
      if (nanos < 0) { throw new IllegalArgumentException("Invalid nanoseconds argument to wait(long, int): " + nanos); }
    }

    this.millis = millis;
    this.nanos = nanos;
  }

  /**
   * @return True if has timeout
   */
  public boolean hasTimeout() {
    return getSignature() != NO_ARGS;
  }

  /**
   * @return True if timeouts are > 0
   */
  public boolean needsToWait() {
    return millis > 0 || nanos > 0;
  }

  /**
   * @return Get millis timeout
   */
  public long getMillis() {
    return millis;
  }

  /**
   * @return Get nanos timeout
   */
  public int getNanos() {
    return nanos;
  }

  /**
   * @return Get method signature
   */
  public Signature getSignature() {
    return this.signature;
  }

  /**
   * Mark seen at current time
   */
  public void mark() {
    mark = System.currentTimeMillis();
  }

  /**
   * Adjust by removing time to wait by now-last mark. 
   */
  public void adjust() {
    if (mark <= LockRequestMessageConsts.UNITIALIZED_WAIT_TIME || signature == NO_ARGS) return;
    long now = System.currentTimeMillis();
    millis -= (now - mark);

    if (millis <= 0) {
      millis = 1;
    }
  }

  public String toString() {
    if (this.signature == NO_ARGS) { return this.signature.toString(); }

    StringBuffer rv = new StringBuffer("wait(");

    if (this.signature == LONG) {
      rv.append(getMillis());
    } else if (this.signature == LONG_INT) {
      rv.append(getMillis()).append(", ").append(getNanos());
    }

    rv.append(")");

    return rv.toString();
  }

  /** Signature for untimed wait */
  public static final Signature NO_ARGS  = new Signature("wait()", 0);
  /** Signature for 1 arg wait() */
  public static final Signature LONG     = new Signature("wait(long)", 1);
  /** Signature for 2 arg wait() */
  public static final Signature LONG_INT = new Signature("wait(long, int)", 2);

  public final static class Signature {
    private final String desc;
    private final int    argCount;

    private Signature(String desc, int numArgs) {
      this.desc = desc;
      this.argCount = numArgs;
    }

    public int getArgCount() {
      return this.argCount;
    }

    public String toString() {
      return desc;
    }

  }

}
