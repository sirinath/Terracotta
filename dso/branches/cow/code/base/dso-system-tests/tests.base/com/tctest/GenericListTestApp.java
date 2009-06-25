/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public class GenericListTestApp extends GenericTransparentApp {

  private static final int LITERAL_VARIANT = 1;
  private static final int OBJECT_VARIANT  = 2;

  public GenericListTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, List.class, 2);
  }

  protected Object getTestObject(String testName) {
    List lists = (List) sharedMap.get("lists");
    return lists.iterator();
  }

  // This method is kind of like a macro, it returns an element (E == element) to be used
  // in the set based on the variant value
  Object E(String val, int variant) {
    switch (variant) {
      case LITERAL_VARIANT: {
        return val;
      }
      case OBJECT_VARIANT: {
        return new Foo(val);
      }
      default: {
        throw new AssertionError("unknown variant: " + variant);
      }
    }

    // unreachable
  }

  protected void setupTestObject(String testName) {
    List lists = new ArrayList();
    // lists.add(new ArrayList());
     lists.add(new CopyOnWriteArrayList());
    // lists.add(new LinkedList());
    // lists.add(new Vector());
    // lists.add(new Stack());
    // lists.add(new MyArrayList());
    // lists.add(new MyArrayList5());
    // lists.add(new MyArrayList6());
    // lists.add(new MyLinkedList());
    // lists.add(new MyVector());
    // lists.add(new MyStack());
    // lists.add(new MyAbstractListSubclass());

    sharedMap.put("lists", lists);
    sharedMap.put("arrayforLinkedList", new Object[2]);
    sharedMap.put("arrayforArrayList", new Object[2]);
    sharedMap.put("arrayforCOWArrayList", new Object[2]);
    sharedMap.put("arrayforVector", new Object[2]);
    sharedMap.put("arrayforStack", new Object[2]);
    sharedMap.put("arrayforAbstractListSubclass", new Object[2]);
    sharedMap.put("arrayforMyArrayList", new Object[2]);
    sharedMap.put("arrayforMyArrayList5", new Object[2]);
    sharedMap.put("arrayforMyArrayList6", new Object[2]);
    sharedMap.put("arrayforMyLinkedList", new Object[2]);
    sharedMap.put("arrayforMyVector", new Object[2]);
    sharedMap.put("arrayforMyStack", new Object[2]);
    sharedMap.put("arrayforMyAbstractListSubclass", new Object[2]);
  }

   void testBasicRemove(List list, boolean validate, int v) {
    System.out.println("variant=" + v + ", " + Thread.currentThread().getName());
    if (validate) {
      System.out.println("validate: " + Thread.currentThread().getName());
      assertEmptyList(list);
    } else {
      synchronized (list) {
        System.out.println("adding: " + Thread.currentThread().getName());
        Object o = E("http://en.wikipedia.org/wiki/PEBKAC", v);
        boolean added = list.add(o);
        Assert.assertTrue(added);
      }

      synchronized (list) {
        System.out.println("removing: " + Thread.currentThread().getName());
        boolean removed = list.remove(E("http://en.wikipedia.org/wiki/PEBKAC", v));
        Assert.assertTrue(removed);
      }
      System.out.println("DONE removing in mutator: "  + Thread.currentThread().getName());
    }
  }

  void bestBasicAdd(List list, boolean validate, int v) {
    if (validate) {
      assertSingleElement(list, E("rollin in my 6-4", v));
    } else {
      synchronized (list) {
        boolean added = list.add(E("rollin in my 6-4", v));
        Assert.assertTrue(added);
      }
    }
  }

  void bestVectorSetSizeGrow(List list, boolean validate, int v) {
    if (!(list instanceof Vector)) { return; }

    int size = 5;
    Vector vector = (Vector) list;

    if (validate) {
      Assert.assertEquals(E("start", v), vector.get(0));
      for (int i = 1; i < size; i++) {
        Object val = vector.get(i);
        Assert.assertNull("element " + i + " is " + val, val);
      }
      Assert.assertEquals(E("end", v), vector.get(size));
    } else {
      synchronized (vector) {
        vector.add(E("start", v));
        vector.setSize(size);
        vector.add(E("end", v));
      }
    }
  }

  void bestVectorSetSizeShrink(List list, boolean validate, int v) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      Assert.assertEquals(E("start", v), vector.get(0));
      Assert.assertEquals(E("end", v), vector.get(1));
    } else {
      synchronized (vector) {
        vector.add(E("start", v));
        vector.add(E("ho hum", v));
        vector.add(E("ho hum2", v));
        vector.add(E("ho hum3", v));

        vector.setSize(1);
        vector.add(E("end", v));
      }
    }
  }

  void bestVectorAddElement(List list, boolean validate, int v) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), vector);
    } else {
      synchronized (vector) {
        vector.addElement(E("first element", v));
        vector.addElement(E("second element", v));
      }
    }
  }

  void bestVectorRetainAll(List list, boolean validate, int v) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("second element", v) }), vector);
    } else {

      synchronized (vector) {
        vector.addElement(E("first element", v));
        vector.addElement(E("second element", v));
        vector.addElement(E("third element", v));

        List retainList = new ArrayList(1);
        retainList.add(E("second element", v));

        vector.retainAll(retainList);
      }
    }
  }

  void bestVectorRemoveAll(List list, boolean validate, int v) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("third element", v) }), vector);
    } else {
      synchronized (vector) {
        vector.addElement(E("first element", v));
        vector.addElement(E("second element", v));
        vector.addElement(E("third element", v));

        List removeList = new ArrayList(1);
        removeList.add(E("second element", v));
        vector.removeAll(removeList);
      }
    }
  }

  void bestVectorRemoveAllElements(List list, boolean validate, int v) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertEmptyList(vector);
    } else {
      synchronized (vector) {
        vector.addElement(E("first element", v));
        vector.addElement(E("second element", v));
        vector.addElement(E("third element", v));

        vector.removeAllElements();
      }
    }
  }

  void bestVectorSetElementAt(List list, boolean validate, int v) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v),
          E("third element", v) }), vector);
    } else {
      synchronized (vector) {
        vector.addElement(E("first element", v));
        vector.addElement(null);
        vector.addElement(E("third element", v));

        vector.setElementAt(E("second element", v), 1);
      }
    }
  }

  void bestVectorInsertElementAt(List list, boolean validate, int v) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v),
          E("third element", v) }), vector);
    } else {
      synchronized (vector) {
        vector.addElement(E("first element", v));
        vector.addElement(E("third element", v));

        vector.insertElementAt(E("second element", v), 1);
      }
    }
  }

  void bestLinkedListRemoveFirst(List list, boolean validate, int v) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertSingleElement(linkedList, E("teck", v));
    } else {
      synchronized (linkedList) {
        linkedList.add(E("timmy", v));
        linkedList.add(E("teck", v));
      }

      synchronized (linkedList) {
        linkedList.removeFirst();
      }
    }
  }

  void bestLinkedListRemoveLast(List list, boolean validate, int v) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertSingleElement(linkedList, E("timmy", v));
    } else {
      synchronized (linkedList) {
        linkedList.add(E("timmy", v));
        linkedList.add(E("teck", v));
      }

      synchronized (linkedList) {
        linkedList.removeLast();
      }
    }
  }

  void bestLinkedListAddFirst(List list, boolean validate, int v) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (linkedList) {
        linkedList.add(E("second element", v));
      }

      synchronized (linkedList) {
        linkedList.addFirst(E("first element", v));
      }
    }
  }

  void bestLinkedListAddLast(List list, boolean validate, int v) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (linkedList) {
        linkedList.add(E("first element", v));
      }

      synchronized (linkedList) {
        linkedList.addLast(E("second element", v));
      }
    }
  }

  void bestBasicAddNull(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { null, null, E("my cat hates you", v), null }), list);
    } else {
      synchronized (list) {
        boolean added;
        added = list.add(null);
        Assert.assertTrue(added);
        added = list.add(null);
        Assert.assertTrue(added);
        added = list.add(E("my cat hates you", v));
        Assert.assertTrue(added);
        added = list.add(null);
        Assert.assertTrue(added);
      }
    }
  }

  void bestBasicAddAt(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("1", v), E("2", v), E("3", v), E("4", v) }), list);
    } else {
      synchronized (list) {
        list.add(0, E("2", v));
      }
      synchronized (list) {
        list.add(0, E("1", v));
      }
      synchronized (list) {
        list.add(2, E("4", v));
      }
      synchronized (list) {
        list.add(2, E("3", v));
      }
    }
  }

  void bestAdd(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("element", v));
      }
    }
  }

  void bestAddAll(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("patty", v), E("calahan", v), E("was", v), E("here", v) }), list);
    } else {
      List toAdd = new ArrayList();
      toAdd.add(E("patty", v));
      toAdd.add(E("calahan", v));
      toAdd.add(E("was", v));
      toAdd.add(E("here", v));

      synchronized (list) {
        list.addAll(toAdd);
      }
    }
  }

  void bestAddAllAt(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("uno", v), E("dos", v), E("tres", v), E("catorce?", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("uno", v));
      }

      List toAdd = new ArrayList();
      toAdd.add(E("dos", v));
      toAdd.add(E("tres", v));
      toAdd.add(E("catorce?", v));

      synchronized (list) {
        list.addAll(1, toAdd);
      }
    }
  }

  void bestClear(List list, boolean validate, int v) {

    if (validate) {
      assertEmptyList(list);
    } else {
      synchronized (list) {
        list.add(E("clear me baby", v));
        list.clear();
      }

      synchronized (list) {
        list.add(E("clear me baby one more time", v));
      }

      synchronized (list) {
        list.clear();
      }
    }
  }

  void bestSetElementAt(List list, boolean validate, int v) {

    if (validate) {
      assertSingleElement(list, E("new", v));
    } else {
      synchronized (list) {
        list.add(E("orig", v));
      }

      synchronized (list) {
        list.set(0, E("new", v));
      }
    }
  }

  void bestRemoveAt(List list, boolean validate, int v) {

    if (validate) {
      Object item0 = list.get(0);
      Object item1 = list.get(1);
      Assert.assertEquals(E("value", v), item0);
      Assert.assertEquals(E("different value", v), item1);
    } else {
      synchronized (list) {
        list.add(E("value", v));
        list.add(E("different value", v));
        list.add(E("value", v));
      }

      synchronized (list) {
        Object prev = list.remove(2);
        Assert.assertEquals(E("value", v), prev);
      }
    }
  }

  void bestRemoveNull1(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), null, E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(null);
        list.add(null);
        list.add(E("third element", v));
      }
      synchronized (list) {
        list.remove(null);
      }
    }
  }

  void bestRemoveNull2(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(null);
        list.add(E("second element", v));
        list.add(null);
      }
      synchronized (list) {
        list.remove(null);
        list.remove(null);
      }
    }
  }

  void bestSubList(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v),
          E("third element", v), E("fourth element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("third element", v));
        list.add(E("fourth element", v));
      }
      List subList = list.subList(1, 2);
      ListIterator listIterator = subList.listIterator();
      synchronized (list) {
        listIterator.add(E("second element", v));
      }
    }
  }

  // void bestRemoveAll(List list, boolean validate, int v) {
  //
  // if (validate) {
  // assertEmptyList(list);
  // } else {
  // synchronized (list) {
  // list.add(E("first element", v));
  // list.add(E("second element", v));
  // }
  // List removeList = new ArrayList(2);
  // removeList.add(E("first element", v));
  // removeList.add(E("second element", v));
  // synchronized (list) {
  // list.removeAll(removeList);
  // }
  // }
  // }

  // void bestRemoveRange(List list, boolean validate, int v) {
  // // using reflection to invoke protected method of a logical subclass does not work.
  // if (list instanceof MyArrayList6) { return; }
  //
  // if (validate) {
  // assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("fourth element", v) }), list);
  // } else {
  // synchronized (list) {
  // list.add(E("first element", v));
  // list.add(E("second element", v));
  // list.add(E("third element", v));
  // list.add(E("fourth element", v));
  // }
  // Class listClass = AbstractList.class;
  // Class[] parameterType = new Class[2];
  // parameterType[0] = Integer.TYPE;
  // parameterType[1] = Integer.TYPE;
  //
  // try {
  // synchronized (list) {
  // Method m = listClass.getDeclaredMethod("removeRange", parameterType);
  // m.setAccessible(true); // suppressing java access checking since removeRange is
  // // a protected method.
  // m.invoke(list, new Object[] { new Integer(1), new Integer(3) });
  // }
  // } catch (Exception e) {
  // // ignore Exception in test.
  // }
  // }
  // }

  void bestToArray(List list, boolean validate, int v) {

    Object[] array = getArray(list);

    if (validate) {
      assertListsEqual(Arrays.asList(array), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
      }
      synchronized (array) {
        Object[] returnArray = list.toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  void bestToArray2(List list, boolean validate, int v) {
    Object[] array = getArray(list);

    if (validate) {
      Assert.assertEquals(list.getClass(), E("January", v), array[0]);
      Assert.assertEquals(list.getClass(), null, array[1]);
    } else {
      synchronized (list) {
        list.add(E("January", v));
      }

      // ensure that the array is bigger than the list size
      // This test case makes sure the array get's null terminated
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(2, array.length);

      // make sure the array contains no nulls
      synchronized (array) {
        Arrays.fill(array, new Object());
      }

      synchronized (array) {
        Object[] returnArray = list.toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  // List Iterator testing methods.
  void bestListIteratorSet1(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("modified first element", v), E("second element", v),
          E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.set(E("modified first element", v));
      }
    }
  }

  void bestListIteratorSet2(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("modified second element", v),
          E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.set(E("modified second element", v));
      }
    }
  }

  void bestListIteratorSetRemove1(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("modified first element", v), E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.remove();
        lIterator.previous();
        lIterator.set(E("modified first element", v));
      }
    }
  }

  void bestListIteratorSetRemove2(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("modified second element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.set(E("modified second element", v));
        lIterator.next();
        lIterator.remove();
      }
    }
  }

  void bestListIteratorDuplicateElementRemove(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
        list.add(E("first element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.next();
        lIterator.remove();
      }
    }
  }

  void bestListIteratorAdd1(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v),
          E("third element", v) }), list);
      // assertListsEqual(Arrays.asList(new Object[] { E("second element", v), E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("second element", v));
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.add(E("first element", v));
      }
    }
  }

  void bestListIteratorAdd2(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v),
          E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.add(E("second element", v));
      }
    }
  }

  void bestListIteratorAddSet1(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("modified first element", v), E("second element", v),
          E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("second element", v));
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.add(E("first element", v));
        lIterator.previous();
        lIterator.set(E("modified first element", v));
      }
    }
  }

  void bestListIteratorAddSet2(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v),
          E("modified third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.add(E("second element", v));
        lIterator.next();
        lIterator.set(E("modified third element", v));
      }
    }
  }

  void bestListIteratorAddSet3(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v),
          E("modified third element", v), E("fourth element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
        list.add(E("fourth element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator(1);
        lIterator.next();
        lIterator.add(E("third element", v));
        lIterator.previous();
        lIterator.set(E("modified third element", v));
      }
    }
  }

  void bestListIteratorAddNull(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { null, null, E("third element", v) }), list);
    } else {
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.add(null);
        lIterator.add(null);
        lIterator.add(E("third element", v));
      }
    }
  }

  void bestListIteratorAddRemove(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("second element", v), E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("second element", v));
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.add(E("first element", v));
        lIterator.previous();
        lIterator.remove();
      }
    }
  }

  void bestListIteratorRemoveNull(List list, boolean validate, int v) {
    if (list instanceof CopyOnWriteArrayList) return;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), null, E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(null);
        list.add(null);
        list.add(E("third element", v));
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.remove();
      }
    }
  }

  // Read only testing methods.
  void bestReadOnlyAdd(List list, boolean validate, int v) {

    if (list instanceof Vector || list instanceof CopyOnWriteArrayList) { return; }

    if (validate) {
      assertEmptyList(list);
    } else {
      synchronized (list) {
        try {
          list.add(E("first element", v));
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException t) {
          // Expected
        }
      }
    }
  }

  void bestReadOnlySet(List list, boolean validate, int v) {
    if (list instanceof Vector || list instanceof CopyOnWriteArrayList) { return; }

    if (validate) {
      assertEmptyList(list);
    } else {
      synchronized (list) {
        try {
          list.set(0, E("first element", v));
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException t) {
          // Expected
        }
      }
    }
  }

  // Setting up for the ReadOnly test for remove.
  void bestSetUpRemove(List list, boolean validate, int v) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
      }
      tryReadOnlyRemove(list, v);
    }
  }

  // tryReadOnlyRemove() goes hand in hand with testSetUpRemove().
  private void tryReadOnlyRemove(List list, int v) {
    if (list instanceof CopyOnWriteArrayList) { return; }

    synchronized (list) {
      try {
        list.remove(E("second element", v));
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  void bestSetUpToArray(List list, boolean validate, int v) {
    if (list instanceof Vector) { return; }

    Object[] array = getArray(list);
    if (validate) {
      assertEmptyObjectArray(array);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
      }
      tryReadOnlyToArray(list);
    }
  }

  void tryReadOnlyToArray(List list) {
    Object[] array = getArray(list);
    synchronized (array) {
      try {
        Object[] returnArray = list.toArray(array);
        Assert.assertTrue(returnArray == array);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for Iterator remove.
  void bestSetUpIteratorRemove(List list, boolean validate, int v) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
      }
      tryReadOnlyIteratorRemove(list);
    }
  }

  // tryReadOnlyIteratorRemove() goes hand in hand with testSetUpIteratorRemove().
  private void tryReadOnlyIteratorRemove(List list) {
    if (list instanceof CopyOnWriteArrayList) { return; }

    synchronized (list) {
      try {
        Iterator iterator = list.iterator();
        iterator.next();
        iterator.remove();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for clear.
  void bestSetUpClear(List list, boolean validate, int v) {
    if (list instanceof Vector || list instanceof CopyOnWriteArrayList) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
      }
      tryReadOnlyClear(list);
    }
  }

  // tryReadOnlyClear() goes hand in hand with testSetUpClear().
  private void tryReadOnlyClear(List list) {
    synchronized (list) {
      try {
        list.clear();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for retainAll.
  void bestSetUpRetainAll(List list, boolean validate, int v) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
      }
      tryReadOnlyRetainAll(list, v);
    }
  }

  // tryReadOnlyRetainAll() goes hand in hand with testSetUpRetainAll().
  private void tryReadOnlyRetainAll(List list, int v) {
    if (list instanceof CopyOnWriteArrayList) { return; }

    synchronized (list) {
      List toRetain = new ArrayList();
      toRetain.add(E("first element", v));
      try {
        list.retainAll(toRetain);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for removeAll.
  void bestSetUpRemoveAll(List list, boolean validate, int v) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
      }
      tryReadOnlyRemoveAll(list, v);
    }
  }

  // tryReadOnlyRemoveAll() goes hand in hand with testSetUpRemoveAll().
  private void tryReadOnlyRemoveAll(List list, int v) {
    if (list instanceof CopyOnWriteArrayList) { return; }

    synchronized (list) {
      List toRemove = new ArrayList();
      toRemove.add(E("first element", v));
      try {
        list.removeAll(toRemove);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException e) {
        // Expected
      }
    }
  }

  void bestListIteratorReadOnlyAdd(List list, boolean validate, int v) {
    if (list instanceof Vector || list instanceof CopyOnWriteArrayList) { return; }

    if (validate) {
      assertEmptyList(list);
    } else {
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        try {
          lIterator.add(E("first element", v));
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException e) {
          // Expected
        }
      }
    }
  }

  void bestCollectionsAddAll(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v),
          E("third element", v) }), list);
    } else {
      synchronized (list) {
        list.addAll(Arrays
            .asList(new Object[] { E("first element", v), E("second element", v), E("third element", v) }));
      }
    }
  }

  // Iterator testing methods.
  // void bestIteratorRemove(List list, boolean validate, int v) {
  //
  // if (validate) {
  // assertListsEqual(Arrays.asList(new Object[] { E("second element", v) }), list);
  // } else {
  // synchronized (list) {
  // list.add(E("first element", v));
  // list.add(E("second element", v));
  // }
  // synchronized (list) {
  // Iterator iterator = list.iterator();
  // Assert.assertEquals(true, iterator.hasNext());
  // iterator.next();
  // try {
  // iterator.remove();
  // } catch (UnsupportedOperationException e2) {
  // // Expected
  // }
  // }
  // }
  // }

  // void bestIteratorDuplicateElementRemove(List list, boolean validate, int v) {
  //
  // if (validate) {
  // assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
  // } else {
  // synchronized (list) {
  // list.add(E("first element", v));
  // list.add(E("second element", v));
  // list.add(E("first element", v));
  // }
  // synchronized (list) {
  // Iterator iterator = list.iterator();
  // Assert.assertEquals(true, iterator.hasNext());
  // iterator.next();
  // iterator.next();
  // iterator.next();
  // try {
  // iterator.remove();
  // } catch (UnsupportedOperationException e2) {
  // // Expected
  // }
  // }
  // }
  // }

  // void bestIteratorRemoveNull(List list, boolean validate, int v) {
  //
  // if (validate) {
  // assertListsEqual(Arrays.asList(new Object[] { E("first element", v), null, E("second element", v) }), list);
  // } else {
  // synchronized (list) {
  // list.add(E("first element", v));
  // list.add(null);
  // list.add(null);
  // list.add(E("second element", v));
  // }
  // synchronized (list) {
  // Iterator iterator = list.iterator();
  // Assert.assertEquals(true, iterator.hasNext());
  // iterator.next();
  // iterator.next();
  // try {
  // iterator.remove();
  // } catch (UnsupportedOperationException e2) {
  // // Expected
  // }
  // }
  // }
  // }

  // Stack specific testing method.
  void bestStackPush(List list, boolean validate, int v) {
    if (!(list instanceof Stack)) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      synchronized (list) {
        Stack s = (Stack) list;
        s.push(E("first element", v));
        s.push(E("second element", v));
      }
    }
  }

  void bestStackPop(List list, boolean validate, int v) {
    if (!(list instanceof Stack)) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v) }), list);
    } else {
      Stack s = (Stack) list;
      synchronized (list) {
        s.push(E("first element", v));
        s.push(E("second element", v));
      }
      synchronized (list) {
        Object o = s.pop();
        Assert.assertEquals(E("second element", v), o);
      }
    }
  }

  void bestAddNonPortableObject(List list, boolean validate, int v) {
    if (!validate) {
      synchronized (list) {
        try {
          list.add(new MyArrayList2());
          throw new AssertionError("Should have thrown a TCNonPortableObjectError.");
        } catch (TCNonPortableObjectError e) {
          // expected
        }
      }
      synchronized (list) {
        try {
          list.add(new MyArrayList3());
          throw new AssertionError("Should have thrown a TCNonPortableObjectError.");
        } catch (TCNonPortableObjectError e) {
          // expected
        }
      }
    }
  }

  private Object getMySubclassArray(List list) {
    if (list instanceof MyArrayList6) { return sharedMap.get("arrayforMyArrayList6"); }
    if (list instanceof MyArrayList5) { return sharedMap.get("arrayforMyArrayList5"); }
    if (list instanceof MyArrayList) { return sharedMap.get("arrayforMyArrayList"); }
    if (list instanceof MyLinkedList) { return sharedMap.get("arrayforMyLinkedList"); }
    if (list instanceof MyVector) { return sharedMap.get("arrayforMyVector"); }
    if (list instanceof MyStack) { return sharedMap.get("arrayforMyStack"); }
    if (list instanceof MyAbstractListSubclass) { return sharedMap.get("arrayforMyAbstractListSubclass"); }
    return null;
  }

  private Object[] getArray(List list) {
    Object o = getMySubclassArray(list);
    if (o != null) { return (Object[]) o; }

    if (list instanceof CopyOnWriteArrayList) { return (Object[]) sharedMap.get("arrayforCOWArrayList"); }
    if (list instanceof LinkedList) { return (Object[]) sharedMap.get("arrayforLinkedList"); }
    if (list instanceof ArrayList) { return (Object[]) sharedMap.get("arrayforArrayList"); }
    if (list instanceof Stack) { // need to check instanceof Stack first before checking instance of Vector
      // as Stack is a subclass of Vector.
      return (Object[]) sharedMap.get("arrayforStack");
    }
    if (list instanceof Vector) { return (Object[]) sharedMap.get("arrayforVector"); }
    if (list instanceof MyAbstractListSubclass) { return (Object[]) sharedMap.get("arrayforAbstractListSubclass"); }
    return null;
  }

  private static void assertEmptyObjectArray(Object[] array) {
    for (int i = 0; i < array.length; i++) {
      Assert.assertNull(array[i]);
    }
  }

  private static void assertEmptyList(List list) {
    System.out.println("assertEmptyList: " + list.size());
    Assert.assertEquals(list.getClass(), 0, list.size());
    Assert.assertTrue(list.getClass(), list.isEmpty());

    int count = 0;
    for (Iterator i = list.iterator(); i.hasNext();) {
      count++;
    }

    Assert.assertEquals(list.getClass(), 0, count);

    for (Iterator i = list.listIterator(); i.hasNext();) {
      count++;
    }

    Assert.assertEquals(list.getClass(), 0, count);
  }

  private static void assertListsEqual(List expect, List actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Assert.assertTrue(expect.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expect));

    for (int i = 0, n = expect.size(); i < n; i++) {
      Assert.assertEquals(expect.get(i), actual.get(i));
    }

    if (expect.isEmpty()) {
      Assert.assertTrue(actual.isEmpty());
    } else {
      Assert.assertFalse(actual.isEmpty());
    }

    for (Iterator iExpect = expect.iterator(), iActual = actual.iterator(); iExpect.hasNext();) {
      Assert.assertEquals(iExpect.next(), iActual.next());
    }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = GenericListTestApp.class.getName();
    config.getOrCreateSpec(testClass);
    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
    config.addIncludePattern(testClass + "$*");
  }

  private static void assertSingleElement(List list, Object obj) {
    Assert.assertEquals(1, list.size());
    Assert.assertEquals(obj, list.get(0));
    Assert.assertFalse(list.isEmpty());
    Assert.assertTrue(list.contains(obj));

    int count = 0;
    for (Iterator i = list.iterator(); i.hasNext();) {
      count++;
      Assert.assertEquals(obj, i.next());
    }
    Assert.assertEquals(1, count);

  }

  private static class MyArrayList extends ArrayList {
    public MyArrayList() {
      super();
    }
  }

  private static class MyArrayList2 extends ArrayList {
    private Vector vector;

    protected void removeRange(int fromIndex, int toIndex) {
      super.removeRange(fromIndex, toIndex);
    }

    public Vector getVector() {
      return vector;
    }
  }

  private static class MyArrayList3 extends ArrayList {
    private Vector vector;

    public void removeRangeLocal(int fromIndex, int toIndex) {
      super.removeRange(fromIndex, toIndex);
    }

    public Vector getVector() {
      return vector;
    }
  }

  private static class MyArrayList4 extends ArrayList {
    //
  }

  private static class MyArrayList5 extends MyArrayList4 {
    //
  }

  private static class MyArrayList6 extends ArrayList {

    int i = 3;

    MyArrayList6() {
      Set s = new HashSet();
      s.add("test");
      new ArrayList(s);
      if (size() != 0) { throw new AssertionError(); }
    }

    MyArrayList6(Set s1) {
      super(s1);
      Set s = new HashSet();
      s.add("test");
      ArrayList l = new ArrayList(s);
      if (size() != 0) { throw new AssertionError(l.size()); }
    }
  }

  private static class MyLinkedList extends LinkedList {
    public MyLinkedList() {
      super();
    }
  }

  private static class MyVector extends Vector {
    public MyVector() {
      super();
    }
  }

  private static class MyStack extends Stack {
    public MyStack() {
      super();
    }
  }

  private static class MyAbstractListSubclass extends AbstractList {
    // This is in here to make sure that a subclass of AbstractList is sharable in DSO, not that this is a good/proper
    // List implementation ;-)

    private ArrayList data = new ArrayList();

    public void add(int index, Object element) {
      data.add(index, element);
    }

    public Object set(int index, Object element) {
      return data.set(index, element);
    }

    public Object get(int index) {
      return data.get(index);
    }

    public int size() {
      return data.size();
    }

    public Object remove(int index) {
      return data.remove(index);
    }

  }

  private static class Foo implements Comparable {
    private final String value;

    Foo(String value) {
      this.value = value;
    }

    public int hashCode() {
      return value.hashCode();
    }

    public boolean equals(Object obj) {
      if (obj instanceof Foo) { return value.equals(((Foo) obj).value); }
      return false;
    }

    public int compareTo(Object o) {
      return value.compareTo(((Foo) o).value);
    }
  }

}
