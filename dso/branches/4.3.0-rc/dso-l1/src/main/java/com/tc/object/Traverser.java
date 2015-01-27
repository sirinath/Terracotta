/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.abortable.AbortedOperationException;
import com.tc.exception.TCRuntimeException;
import com.tc.net.GroupID;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Generic Object traverser. Initial use for this is to add any unmanaged objects to the managed object tree
 * 
 * @author steve
 */
public class Traverser {
  private static final TraverseTest    NULL_TEST = new NullTraverseTest();

  private final PortableObjectProvider portableObjectProvider;

  public Traverser(PortableObjectProvider portableObjectProvider) {
    this.portableObjectProvider = portableObjectProvider;
  }

  private void addReferencedObjects(Map toBeVisited, Object start, Map visited, TraverseTest traverseTest) {
    for (Class clazz = start.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
      TraversedReferences portableObjects = new TraversedReferencesImpl();
      portableObjects = portableObjectProvider.getPortableObjects(clazz, start, portableObjects);

      for (Iterator<TraversedReference> i = portableObjects.iterator(); i.hasNext();) {
        try {
          TraversedReference currentReference = i.next();
          Object currentObject = currentReference.getValue();

          if (doNotTraverse(traverseTest, visited, currentObject)) {
            continue;
          }

          traverseTest.checkPortability(currentReference, start.getClass());

          toBeVisited.put(currentObject, null);
        } catch (IllegalArgumentException e) {
          throw new TCRuntimeException(e);
        }
      }
    }

  }

  private boolean doNotTraverse(TraverseTest traverseTest, Map visited, Object current) {
    if (current == null) { return true; }

    if (visited.containsKey(current)) { return true; }

    if (!traverseTest.shouldTraverse(current)) { return true; }

    return false;
  }

  // package protected - used for tests only
  void traverse(Object object, TraversalAction action) throws AbortedOperationException {
    traverse(object, NULL_TEST, action, GroupID.NULL_ID);
  }

  public void traverse(Object object, TraverseTest traverseTest, TraversalAction action,
                       GroupID gid) throws AbortedOperationException {
    Map visited = new IdentityHashMap();
    List toAdd = new ArrayList();

    visited.put(object, null);

    IdentityHashMap toBeVisited = new IdentityHashMap();
    addReferencedObjects(toBeVisited, object, visited, traverseTest);
    toAdd.add(object);

    while (!toBeVisited.isEmpty()) {
      for (Iterator i = new IdentityHashMap(toBeVisited).keySet().iterator(); i.hasNext();) {
        Object obj = i.next();
        visited.put(obj, null);
        toBeVisited.remove(obj);
        addReferencedObjects(toBeVisited, obj, visited, traverseTest);
        toAdd.add(obj); // action.visit() to be taken place after addReferencedObjects() so that
        // the manager of the referenced objects will only be set after the referenced
        // objects are obtained.
      }
    }
    action.visit(toAdd, gid);
  }
}
