/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

/**
 * Interface for TC-instrumented java.lang.String, to manage additional behavior such
 * as compression
 */
public interface JavaLangString {

  /**
   * Indicates whether TC-instrumented String is internally compressed or not
   * @return whether String is compressed or not
   */
  public boolean __tc_isCompressed();
  
  /**
   * Force String to decompress if it was compressed
   * 
   * @return true if String was compressed
   */
  public void __tc_decompress();

}
