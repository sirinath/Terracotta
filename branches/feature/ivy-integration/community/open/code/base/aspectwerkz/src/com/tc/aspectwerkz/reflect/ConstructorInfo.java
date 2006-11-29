/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

/**
 * Interface for the constructor info implementations.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bon�r </a>
 */
public interface ConstructorInfo extends MemberInfo {
  /**
   * Returns the parameter types.
   *
   * @return the parameter types
   */
  ClassInfo[] getParameterTypes();

  /**
   * Returns the exception types.
   *
   * @return the exception types
   */
  ClassInfo[] getExceptionTypes();
}