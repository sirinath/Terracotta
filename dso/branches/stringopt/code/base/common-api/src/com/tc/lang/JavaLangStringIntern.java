/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;

/**
 * Interface for java.lang.String to manage the interned string objects
 */
public interface JavaLangStringIntern {

  /**
   * Check whether the String is interned
   * 
   * @return true if it is interned string
   */
  public boolean __tc_isInterned();

  /**
   * Call intern and mark the String instance as interned.
   * 
   * @return Interned string
   */
  public String __tc_intern();
}
