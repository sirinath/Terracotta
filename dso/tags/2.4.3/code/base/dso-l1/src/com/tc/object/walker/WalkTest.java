/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.walker;

public interface WalkTest {
  boolean shouldTraverse(MemberValue val);
  boolean includeFieldsForType(Class type);
}
