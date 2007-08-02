/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

public interface ILiteralValues {

  public final static String ENUM_CLASS_DOTS              = "java.lang.Enum";
  // XXX:: If you are adding more types, please see PhysicalStateClassLoader and DNAEncoding
  public final static int    INTEGER                      = 0;
  public final static int    LONG                         = 1;
  public final static int    CHARACTER                    = 2;
  public final static int    FLOAT                        = 3;
  public final static int    DOUBLE                       = 4;
  public final static int    BYTE                         = 5;
  public final static int    STRING                       = 6;
  public final static int    BOOLEAN                      = 7;
  public final static int    SHORT                        = 8;
  public final static int    ARRAY                        = 9;
  public final static int    OBJECT                       = 10;
  public final static int    OBJECT_ID                    = 11;
  public final static int    STRING_BYTES                 = 12;
  public final static int    JAVA_LANG_CLASS              = 13;
  public final static int    JAVA_LANG_CLASS_HOLDER       = 14;
  public final static int    STACK_TRACE_ELEMENT          = 15;
  public final static int    BIG_INTEGER                  = 16;
  public final static int    BIG_DECIMAL                  = 17;
  public final static int    JAVA_LANG_CLASSLOADER        = 18;
  public final static int    JAVA_LANG_CLASSLOADER_HOLDER = 19;
  public final static int    ENUM                         = 20;
  public final static int    ENUM_HOLDER                  = 21;
  public final static int    CURRENCY                     = 22;

  public abstract int valueFor(Object pojo);

  public abstract boolean isLiteral(String className);

  public abstract boolean isLiteralInstance(Object obj);

  public abstract int valueForClassName(String className);

}