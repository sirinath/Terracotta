/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.field;

import com.tc.object.TCClass;

/**
 * Terracotta managed information attached to a field
 * @author orion
 */
public interface TCField {
  /**
   * Returns the TCClass which declared this field.
   * @return The declaring class
   */
  public TCClass getDeclaringTCClass();

  /**
   * @return True if field is final
   */
  public boolean isFinal();

  /**
   * @return True if field is portable
   */
  public boolean isPortable();

  /**
   * @return True if field is array
   */
  public boolean isArray();

  /**
   * @return True if field is reference to another object
   */
  public boolean canBeReference();
  
  /**
   * @return Field name
   */
  public String getName();
}