/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.util.AbstractIdentifier;

import java.io.Serializable;

/**
 * Object representing the ID of any managed object
 */
public class ObjectID extends AbstractIdentifier implements Serializable {

  /**
   * The NULL ObjectID
   */
  public final static ObjectID NULL_ID = new ObjectID();

  // Only the last 7 bytes are used for object id, the 1st byte represent group id.
  // This still holds about 72 trillion object (72057594037927935) and 128 groups
  public final static long     MAX_ID  = ((long) 1) << 56 - 1;

  /**
   * Create an ObjectID with the specified ID
   * 
   * @param id The id value, >= 0
   */
  public ObjectID(long id) {
    super(id);
  }

  /**
   * Create a "null" ObjectID.
   */
  private ObjectID() {
    super();
  }

  public ObjectID(long oid, int gid) {
    super(getObjectID(oid, gid));
  }

  private static long getObjectID(long oid, int gid) {
    if (gid > 127 || oid > MAX_ID) throw new AssertionError("Currently only supports upto 128 Groups and " + MAX_ID
                                                            + " objects : " + gid + "," + oid);
    long gidLong = (long) gid & 0xFF;
    gidLong = gidLong << 56;
    oid = gidLong | oid;
    return oid;
  }

  public String getIdentifierType() {
    return "ObjectID";
  }

  public int getGroupID() {
    long oid = toLong();
    if (oid <= MAX_ID) { return 0; }
    long gid = oid & 0xFF00000000000000L;
    gid = gid >> 56;
    return (int) gid;
  }

  public long getObjectID() {
    long oid = toLong();
    if (oid <= MAX_ID) { return oid; }
    return (oid & 0x00FFFFFFFFFFFFFFL);
  }

  public String toString() {
    long oid = toLong();
    if (oid <= MAX_ID) { return super.toString(); }
    return getIdentifierType() + "=" + "[" + getGroupID() + ":" + getObjectID() + "]";
  }

}