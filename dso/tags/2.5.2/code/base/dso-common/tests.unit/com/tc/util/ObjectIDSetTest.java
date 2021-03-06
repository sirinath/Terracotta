/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ObjectIDSetTest extends TCTestCase {

  interface SetCreator {
    public Set create();

    public Set create(Collection c);
  }

  /**
   * This test is disabled since it is too slow and times out in slow boxes.
   */
  public void DISABLEDtestObjectIDSet() {
    SetCreator creator = new SetCreator() {
      public Set create() {
        return new ObjectIDSet();
      }

      public Set create(Collection c) {
        return new ObjectIDSet(c);
      }

    };
    basicTest(creator);
    iteratorRemoveTest(creator);
  }

  public void basicTest(SetCreator creator) {
    basicTest(creator, 100000, 100000);
    basicTest(creator, 500000, 100000);
    basicTest(creator, 100000, 1000000);
  }

  public void basicTest(SetCreator creator, int distRange, int iterationCount) {
    long test_start = System.currentTimeMillis();
    Set s = new HashSet();
    Set small = creator.create();
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
    Set copy = creator.create(all);
    System.err.println("Time to add all IDs from a collection to a new " + cname + " = "
                       + (System.currentTimeMillis() - start) + " ms");
    start = System.currentTimeMillis();
    Set clone = creator.create(small);
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
    ObjectIDSet2 org = new ObjectIDSet2(s);
    assertEquals(s, org);

    ObjectIDSet2 ser = serializeAndRead(org);
    assertEquals(s, ser);
    assertEquals(org, ser);
  }

  private ObjectIDSet2 serializeAndRead(ObjectIDSet2 org) throws Exception {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(org);
    System.err.println("Written ObjectIDSet2 size : " + org.size());
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    ObjectIDSet2 oids = (ObjectIDSet2) oi.readObject();
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

  public void testObjectIDSet2() {
    SetCreator creator = new SetCreator() {
      public Set create() {
        return new ObjectIDSet2();
      }

      public Set create(Collection c) {
        return new ObjectIDSet2(c);
      }

    };
    basicTest(creator);
  }

  public void iteratorRemoveTest(SetCreator creator) {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    iteratorRemoveTest(creator, seed);
  }

  private void iteratorRemoveTest(SetCreator creator, long seed) {
    Set all = new HashSet();
    Set oidSet = creator.create();
    System.err.println("Running iteratorRemoveTest for " + oidSet.getClass().getName() + " and seed is " + seed);
    Random r = new Random(seed);
    for (int i = 0; i < 50000; i++) {
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

  // See the comment above
  public void DISABLEDtestFailedCase() {
    System.err.println("\nRunning testFailedCase()... ");
    SetCreator creator = new SetCreator() {
      public Set create() {
        return new ObjectIDSet();
      }

      public Set create(Collection c) {
        return new ObjectIDSet(c);
      }

    };
    long seed = 1576555335886137186L;
    iteratorRemoveTest(creator, seed);
  }

}
