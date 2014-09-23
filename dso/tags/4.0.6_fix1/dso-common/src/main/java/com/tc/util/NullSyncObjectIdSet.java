/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.text.PrettyPrinter;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public class NullSyncObjectIdSet extends AbstractSet implements SyncObjectIdSet {

  @Override
  public void startPopulating() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stopPopulating(ObjectIDSet fullSet) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(Object obj) {
    return true;
  }

  @Override
  public boolean contains(Object o) {
    return true;
  }

  @Override
  public boolean removeAll(Collection ids) {
    return true;
  }

  @Override
  public boolean remove(Object o) {
    return true;
  }

  @Override
  public Iterator iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public ObjectIDSet snapshot() {
    throw new UnsupportedOperationException();
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out;
  }

  @Override
  public int addAndGetSize(ObjectID obj) {
    return 0;
  }

  @Override
  public void waitUntilFinishedPopulating() {
    //
  }
}
