/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.TCObject;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;

public class ManagerInternalUtil {

  public static final String           CLASS                 = "com/tc/object/bytecode/ManagerInternalUtil";

  private static final ManagerInternal NULL_MANAGER_INTERNAL = new NullManagerInternal();

  public static TCObject lookupExistingOrNull(Object obj) {
    return getManager().lookupExistingOrNull(obj);
  }

  public static TCObject lookupArrayTCObjectOrNull(Object array) {
    return getManager().lookupArrayTCObjectOrNull(array);
  }

  public static void charArrayCopy(final char[] src, final int srcPos, final char[] dest, final int destPos,
                                   final int length, final TCObject tco) {
    getManager().charArrayCopy(src, srcPos, dest, destPos, length, tco);
  }

  /**
   * Begin volatile lock by field offset in the class
   * 
   * @param pojo Instance containing field
   * @param fieldOffset Field offset in pojo
   * @param type Lock level
   */
  public static void beginVolatile(final Object pojo, final long fieldOffset, final int type) {
    TCObject tco = lookupExistingOrNull(pojo);
    beginVolatile(tco, tco.getFieldNameByOffset(fieldOffset), type);
  }

  /**
   * Commit volatile lock by field offset in the class
   * 
   * @param pojo Instance containing field
   * @param fieldOffset Field offset in pojo
   */
  public static void commitVolatile(final Object pojo, final long fieldOffset, final int type) {
    TCObject tco = lookupExistingOrNull(pojo);
    commitVolatile(tco, tco.getFieldNameByOffset(fieldOffset), type);
  }

  /**
   * Begin volatile lock
   * 
   * @param tco TCObject to lock
   * @param fieldName Field name holding volatile object
   * @param type Lock type
   */
  public static void beginVolatile(final TCObject tco, final String fieldName, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(tco, fieldName);
    mgr.lock(lock, LockLevel.fromInt(type));
  }

  /**
   * Commit volatile lock
   * 
   * @param tco Volatile object
   * @param fieldName Field holding the volatile object
   */
  public static void commitVolatile(final TCObject tco, final String fieldName, final int type) {
    Manager mgr = getManager();
    LockID lock = mgr.generateLockIdentifier(tco, fieldName);
    mgr.unlock(lock, LockLevel.fromInt(type));
  }

  private static ManagerInternal getManager() {
    Manager rv = ManagerUtil.getManager();

    if (rv instanceof ManagerInternal) { return (ManagerInternal) rv; }

    return NULL_MANAGER_INTERNAL;
  }

}
