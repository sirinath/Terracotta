/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class ObjectIDSetTest extends TCTestCase {

  public Set create() {
    return new ObjectIDSet();
  }

  public Set create(Collection c) {
    return new ObjectIDSet(c);
  }

  public void basicTest() {
    basicTest(100000, 100000);
    basicTest(500000, 100000);
    basicTest(100000, 1000000);
  }

  public void testSortedSetObjectIDSet() throws Exception {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    System.err.println("SORTED TEST : Seed for Random is " + seed);
    Random r = new Random(seed);
    TreeSet ts = new TreeSet();
    SortedSet oids = new ObjectIDSet();
    for (int i = 0; i < 100000; i++) {
      long l = r.nextLong();
      ObjectID id = new ObjectID(l);
      boolean b1 = ts.add(id);
      boolean b2 = oids.add(id);
      assertEquals(b1, b2);
      assertEquals(ts.size(), oids.size());
    }

    // verify sorted
    Iterator i = ts.iterator();
    for (Iterator j = oids.iterator(); j.hasNext();) {
      ObjectID oid1 = (ObjectID) i.next();
      ObjectID oid2 = (ObjectID) j.next();
      assertEquals(oid1, oid2);
    }
  }

  public void basicTest(int distRange, int iterationCount) {
    long test_start = System.currentTimeMillis();
    Set s = new HashSet();
    Set small = create();
    String cname = small.getClass().getName();
    System.err.println("Running tests for " + cname + " distRange = " + distRange + " iterationCount = "
                       + iterationCount);
    assertTrue(small.isEmpty());
    assertTrue(small.size() == 0);
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    System.err.println("Seed for Random is " + seed);
    Random r = new Random(seed);
    for (int i = 0; i < iterationCount; i++) {
      long l = r.nextInt(distRange);
      ObjectID id = new ObjectID(l);
      s.add(id);
      small.add(id);
      assertEquals(s.size(), small.size());
    }
    Iterator sit = small.iterator();
    List all = new ArrayList();
    all.addAll(s);
    while (sit.hasNext()) {
      ObjectID i = (ObjectID) sit.next();
      Assert.eval("FAILED:" + i.toString(), s.remove(i));
    }
    Assert.eval(s.size() == 0);

    // test retain all
    Set odds = new HashSet();
    Set evens = new HashSet();
    for (int i = 0; i < all.size(); i++) {
      if (i % 2 == 0) {
        evens.add(all.get(i));
      } else {
        odds.add(all.get(i));
      }
    }
    boolean b = small.retainAll(odds);
    assertTrue(b);
    assertEquals(odds, small);
    b = small.retainAll(evens);
    assertTrue(b);
    assertEquals(0, small.size());
    small.addAll(all); // back to original state

    // test new set creation (which uses cloning
    long start = System.currentTimeMillis();
    Set copy = create(all);
    System.err.println("Time to add all IDs from a collection to a new " + cname + " = "
                       + (System.currentTimeMillis() - start) + " ms");
    start = System.currentTimeMillis();
    Set clone = create(small);
    System.err.println("Time to add all IDs from an ObjectIDSet to a new " + cname + " = "
                       + (System.currentTimeMillis() - start) + " ms");

    Collections.shuffle(all);
    for (Iterator i = all.iterator(); i.hasNext();) {
      ObjectID rid = (ObjectID) i.next();
      Assert.eval(small.contains(rid));
      Assert.eval(clone.contains(rid));
      Assert.eval(copy.contains(rid));
      if (!small.remove(rid)) { throw new AssertionError("couldn't remove:" + rid); }
      if (small.contains(rid)) { throw new AssertionError(rid); }
      if (!clone.remove(rid)) { throw new AssertionError("couldn't remove:" + rid); }
      if (clone.contains(rid)) { throw new AssertionError(rid); }
      if (!copy.remove(rid)) { throw new AssertionError("couldn't remove:" + rid); }
      if (copy.contains(rid)) { throw new AssertionError(rid); }
    }
    for (Iterator i = all.iterator(); i.hasNext();) {
      ObjectID rid = (ObjectID) i.next();
      Assert.eval(!small.contains(rid));
      if (small.remove(rid)) { throw new AssertionError("shouldn't have removed:" + rid); }
      if (small.contains(rid)) { throw new AssertionError(rid); }
      if (clone.remove(rid)) { throw new AssertionError("shouldn't have removed:" + rid); }
      if (clone.contains(rid)) { throw new AssertionError(rid); }
      if (copy.remove(rid)) { throw new AssertionError("shouldn't have removed:" + rid); }
      if (copy.contains(rid)) { throw new AssertionError(rid); }
    }
    Assert.eval(s.size() == 0);
    Assert.eval(small.size() == 0);
    Assert.eval(copy.size() == 0);
    Assert.eval(clone.size() == 0);
    System.err.println("Time taken to run basic Test for " + small.getClass().getName() + " is "
                       + (System.currentTimeMillis() - test_start) + " ms");
  }

  public void testSerializationObjectIDSet2() throws Exception {
    for (int i = 0; i < 20; i++) {
      Set s = createRandomSetOfObjectIDs();
      serializeAndVerify(s);
    }
  }

  private void serializeAndVerify(Set s) throws Exception {
    ObjectIDSet org = new ObjectIDSet(s);
    assertEquals(s, org);

    ObjectIDSet ser = serializeAndRead(org);
    assertEquals(s, ser);
    assertEquals(org, ser);
  }

  private ObjectIDSet serializeAndRead(ObjectIDSet org) throws Exception {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(org);
    System.err.println("Written ObjectIDSet2 size : " + org.size());
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    ObjectIDSet oids = (ObjectIDSet) oi.readObject();
    System.err.println("Read  ObjectIDSet2 size : " + oids.size());
    return oids;
  }

  private Set createRandomSetOfObjectIDs() {
    Set s = new HashSet();
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    System.err.println("Random Set creation : Seed for Random is " + seed);
    Random r = new Random(seed);
    for (int i = 0; i < r.nextInt(100000); i++) {
      s.add(new ObjectID(r.nextLong()));
    }
    System.err.println("Created a set of size : " + s.size());
    return s;
  }

  public void testObjectIDSet() {
    basicTest();
  }

  public void testObjectIDSetDump() {
    ObjectIDSet s = new ObjectIDSet();
    System.err.println(" toString() : " + s);

    for (int i = 0; i < 100; i++) {
      s.add(new ObjectID(i));
    }
    System.err.println(" toString() : " + s);

    for (int i = 0; i < 100; i += 2) {
      s.remove(new ObjectID(i));
    }
    System.err.println(" toString() : " + s);

  }

  public void testObjectIdSetConcurrentModification() {
    ObjectIDSet objIdSet = new ObjectIDSet();
    int num = 0;
    for (num = 0; num < 50; num++) {
      objIdSet.add(new ObjectID(num));
    }

    Iterator iterator = objIdSet.iterator();
    objIdSet.add(new ObjectID(num));
    try {
      iterateElements(iterator);
      throw new AssertionError("We should have got the ConcurrentModificationException");
    } catch (ConcurrentModificationException cme) {
      System.out.println("Caught Expected Exception " + cme.getClass().getName());
    }

    iterator = objIdSet.iterator();
    objIdSet.remove(new ObjectID(0));
    try {
      iterateElements(iterator);
      throw new AssertionError("We should have got the ConcurrentModificationException");
    } catch (ConcurrentModificationException cme) {
      System.out.println("Caught Expected Exception " + cme.getClass().getName());
    }

    iterator = objIdSet.iterator();
    objIdSet.clear();
    try {
      iterateElements(iterator);
      throw new AssertionError("We should have got the ConcurrentModificationException");
    } catch (ConcurrentModificationException cme) {
      System.out.println("Caught Expected Exception " + cme.getClass().getName());
    }

  }

  private long iterateElements(Iterator iterator) throws ConcurrentModificationException {
    return iterateElements(iterator, -1);
  }

  private long iterateElements(Iterator iterator, long count) throws ConcurrentModificationException {
    long itrCount = 0;
    while ((iterator.hasNext()) && (count < 0 || itrCount < count)) {
      itrCount++;
      System.out.print(((ObjectID) iterator.next()).toLong() + ", ");
    }
    System.out.print("\n\n");
    return itrCount;
  }

  public void testObjectIDSetIteratorFullRemove() {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();

    Set all = new HashSet();
    Set oidSet = create();
    System.err.println("Running iteratorRemoveTest for " + oidSet.getClass().getName() + " and seed is " + seed);
    Random r = new Random(seed);
    for (int i = 0; i < 5000; i++) {
      long l = r.nextInt(100000);
      ObjectID id = new ObjectID(l);
      all.add(id);
      oidSet.add(id);
    }

    for (Iterator i = all.iterator(); i.hasNext();) {
      ObjectID rid = (ObjectID) i.next();
      Assert.eval(oidSet.contains(rid));
      for (Iterator j = oidSet.iterator(); j.hasNext();) {
        ObjectID crid = (ObjectID) j.next();
        if (crid.equals(rid)) {
          j.remove();
          break;
        }
      }
    }
    Assert.eval(oidSet.size() == 0);
  }

  public void testObjectIDSetIteratorSparseRemove() {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    Set oidSet = create();
    System.err.println("Running iteratorRemoveTest for " + oidSet.getClass().getName() + " and seed is " + seed);
    Random r = new Random(seed);
    for (int i = 0; i < 1000; i++) {
      ObjectID id;
      do {
        long l = r.nextInt(20000);
        id = new ObjectID(l);
      } while (oidSet.contains(id));
      oidSet.add(id);
    }

    // check if ObjectIDSet has been inited with 1000 elements
    Iterator oidSetIterator = oidSet.iterator();
    assertEquals(1000, iterateElements(oidSetIterator));

    long visitedCount = 0;
    long removedCount = 0;
    oidSetIterator = oidSet.iterator();

    // visit first 100 elements
    visitedCount += iterateElements(oidSetIterator, 100);
    assertEquals(100, visitedCount);

    // remove the 100th element
    oidSetIterator.remove();
    removedCount += 1;

    // visit next 100 elements
    visitedCount += iterateElements(oidSetIterator, 100);
    assertEquals(100 + 100, visitedCount);

    // remove the 200th element
    oidSetIterator.remove();
    removedCount += 1;

    // visit next 100 elements
    visitedCount += iterateElements(oidSetIterator, 100);
    assertEquals(100 + 100 + 100, visitedCount);

    // visit rest of the elements
    visitedCount += iterateElements(oidSetIterator);
    assertEquals(1000, visitedCount);

    // check the backing Set for removed elements
    oidSetIterator = oidSet.iterator();
    long totalElements = iterateElements(oidSetIterator);
    assertEquals((visitedCount - removedCount), totalElements);
  }

  public void testObjectIDSetIteratorRemoveSpecailCases() {
    List longList = new ArrayList();
    longList.add(new ObjectID(25));
    longList.add(new ObjectID(26));
    longList.add(new ObjectID(27));
    longList.add(new ObjectID(28));
    longList.add(new ObjectID(9));
    longList.add(new ObjectID(13));
    longList.add(new ObjectID(12));
    longList.add(new ObjectID(14));
    longList.add(new ObjectID(18));
    longList.add(new ObjectID(2));
    longList.add(new ObjectID(23));
    longList.add(new ObjectID(47));
    longList.add(new ObjectID(35));
    longList.add(new ObjectID(10));
    longList.add(new ObjectID(1));
    longList.add(new ObjectID(4));
    longList.add(new ObjectID(15));
    longList.add(new ObjectID(8));
    longList.add(new ObjectID(56));
    longList.add(new ObjectID(11));
    longList.add(new ObjectID(10));
    longList.add(new ObjectID(33));
    longList.add(new ObjectID(17));
    longList.add(new ObjectID(29));
    longList.add(new ObjectID(19));
    // Data : 1 2 4 8 9 10 11 12 13 14 15 17 18 19 23 25 26 27 28 29 33 35 47 56

    /**
     * ObjectIDSet { (oids:ranges) = 24:10 , compression ratio = 1.0 } [ Range(1,2) Range(4,4) Range(8,15) Range(17,19)
     * Range(23,23) Range(25,29) Range(33,33) Range(35,35) Range(47,47) Range(56,56)]
     */

    Set objectIDSet = create(longList);
    int totalElements = longList.size() - 1;

    Iterator i = objectIDSet.iterator();
    assertEquals(totalElements, iterateElements(i));

    List longSortList = new ArrayList();
    i = objectIDSet.iterator();
    while (i.hasNext()) {
      longSortList.add(i.next());
    }

    // remove first element in a range. eg: 8 from (8,15)
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(8)) + 1, 9);
    objectIDSet.add(new ObjectID(8)); // get back to original state

    // remove last element in a range. eg: 19 from (17,19)
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(19)) + 1, 23);
    objectIDSet.add(new ObjectID(19));

    // remove the only element in the range. eg: 33 from (33,33)
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(33)) + 1, 35);
    objectIDSet.add(new ObjectID(33));

    // remove the least element
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(1)) + 1, 2);
    objectIDSet.add(new ObjectID(1));

    // remove the max element; element will be removed, but while going to next element, exception expected
    try {
      removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(56)) + 1, -99);
      throw new AssertionError("Expected to throw an exception");
    } catch (NoSuchElementException noSE) {
      // expected
    } finally {
      objectIDSet.add(new ObjectID(56));
    }

    // remove the non existing element; exception expected
    try {
      removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(16)) + 1, -99);
      throw new AssertionError("Expected to throw an exception");
    } catch (IllegalStateException ise) {
      // expected
    }

    i = objectIDSet.iterator();
    assertEquals(5, iterateElements(i, 5));
    objectIDSet.add(new ObjectID(99));
    try {
      assertEquals(5, iterateElements(i, 1));
      throw new AssertionError("Expected to throw an exception");
    } catch (ConcurrentModificationException cme) {
      // expected
    } finally {
      objectIDSet.remove(new ObjectID(99));
    }

  }

  private void removeElementFromIterator(Iterator i, int totalElements, long indexOfRemoveElement,
                                         int nextExpectedElement) {
    long visitedElements = 0;
    visitedElements += iterateElements(i, indexOfRemoveElement);
    i.remove();
    assertEquals(nextExpectedElement, ((ObjectID) i.next()).toLong());
    visitedElements += iterateElements(i);
    assertEquals(visitedElements, totalElements - 1);
  }
}
