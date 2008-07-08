/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class SleepycatPersistableSetTest extends TCTestCase {

  private SleepycatPersistableSet set = null;

  protected void setUp() throws Exception {
    super.setUp();
    set = new SleepycatPersistableSet(new ObjectID(12));
  }

  public void testBasic() {
    final int NUMBERS_ADDED = 250 * 4;

    addNumbers(0, NUMBERS_ADDED);
    assertSize(NUMBERS_ADDED);

    addNumbers(0, NUMBERS_ADDED);
    assertSize(NUMBERS_ADDED);

    clearSet();
    assertSize(0);

    addAllFromCollection(0, NUMBERS_ADDED);
    assertSize(NUMBERS_ADDED);

    assertContains(0, NUMBERS_ADDED);
    assertContainsAllAndEquals(0, NUMBERS_ADDED);

    checkIterator(0, NUMBERS_ADDED);
    checkRemove(3 * NUMBERS_ADDED / 4, NUMBERS_ADDED / 4, 3 * NUMBERS_ADDED / 4);
    checkRemoveAll(NUMBERS_ADDED / 2, NUMBERS_ADDED / 4, NUMBERS_ADDED / 2);
    assertSize(NUMBERS_ADDED / 2);

    checkRetainAll(0, NUMBERS_ADDED / 4, NUMBERS_ADDED / 4);
    checkToArray(0, NUMBERS_ADDED / 4);
  }

  private void clearSet() {
    set.clear();
    assertEmpty();
  }

  private void assertContains(int start, int length) {
    for (int i = start; i < start + length; i++)
      assertTrue(set.contains(new Node(i)));
  }

  private void assertContainsAllAndEquals(int start, int length) {
    HashSet tempSet = new HashSet();
    for (int i = start; i < start + length; i++)
      tempSet.add(new Node(i));

    assertContainsAll(tempSet);
    assertEquals(tempSet);
  }

  private void assertContainsAll(Collection collection) {
    assertTrue(set.containsAll(collection));
  }

  public void assertEquals(Collection collection) {
    assertTrue(set.equals(collection));
  }

  private void assertEmpty() {
    assertTrue(set.isEmpty());
  }

  public void checkIterator(int start, int length) {
    Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++)
      vector.add(new Node(i));

    Iterator iter = set.iterator();
    assertNotNull(iter);

    while (iter.hasNext()) {
      assertTrue(vector.contains(iter.next()));
    }
  }

  private void checkRemove(int start, int length, int expectedLeft) {
    for (int i = start; i < start + length; i++)
      set.remove(new Node(i));
    assertSize(expectedLeft);
  }

  private void checkRemoveAll(int start, int length, int expectedLeft) {
    Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++)
      vector.add(new Node(i));

    set.removeAll(vector);
    assertSize(expectedLeft);
  }

  private void checkRetainAll(int start, int length, int expectedLeft) {
    Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++)
      vector.add(new Node(i));

    set.retainAll(vector);
    assertSize(expectedLeft);
  }

  private void assertSize(int expected) {
    assertEquals(expected, set.size());
  }

  private void checkToArray(int start, int length) {
    Vector vector = new Vector(length);
    for (int i = start; i < start + length; i++)
      vector.add(new Node(i));

    Object[] objArray = set.toArray();
    assertEquals(length, objArray.length);

    for (int i = 0; i < length; i++)
      assertTrue(vector.contains(objArray[i]));
  }

  private void addNumbers(int start, int numbersToBeAdded) {
    for (int i = start; i < start + numbersToBeAdded; i++) {
      set.add(new Node(i));
    }
  }

  private void addAllFromCollection(int start, int numbersToBeAdded) {
    ArrayList list = new ArrayList(numbersToBeAdded);
    for (int i = start; i < start + numbersToBeAdded; i++)
      list.add(new Node(i));

    set.addAll(list);
  }

  private class Node {
    private int i;

    public Node(int i) {
      this.i = i;
    }

    public int getNumber() {
      return i;
    }

    @Override
    public boolean equals(Object obj) {
      Node number = (Node) obj;
      return number.i == this.i;
    }

    @Override
    public int hashCode() {
      return i;
    }
  }
}
