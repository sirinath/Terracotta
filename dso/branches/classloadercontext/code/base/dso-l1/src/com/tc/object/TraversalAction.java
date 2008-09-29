/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import java.util.List;

public interface TraversalAction {

  /**
   * {@link Traverser} will call this method, passing in a list of
   * referenced portable POJOs.
   * @param objects a list of POJOs gathered by traversing the
   * distributed object graph
   * @param v the parameter passed in to {@link Traverser#traverse(Object)}
   */
  public void visit(List objects, Object v);
}