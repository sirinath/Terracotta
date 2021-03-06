/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.util;

import sun.misc.MessageUtils;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

import java.util.Map;
import java.util.WeakHashMap;

public class IdentityWeakHashMap extends WeakHashMap {

  static {
    /**
     * This static block is to validate the WeakHashMap superclass is the DSO instrumented version.
     * If not, this class will function incorrectly.
     */
    IdentityWeakHashMap m = new IdentityWeakHashMap();
    Integer i1 = new Integer(10); // strongly reference the Integer object so that the garbage collector
    Integer i2 = new Integer(10); // will not kick in before the check for m.size().
    m.put(i1, "first");
    m.put(i2, "second");
    if (m.size() != 2) {
      ExceptionWrapper wrapper = new ExceptionWrapperImpl();
      String errorMsg = wrapper
          .wrap("WeakHashMap does not seem to contain the instrumented method.\n "
                + "IdentityWeakHashMap can only be used with the Terracotta instrumented version of WeakHashMap. \n"
                + "One possible cause is that WeakHashMap is not contained in the boot jar.");
      MessageUtils.toStderr(errorMsg);
      throw new AssertionError(errorMsg);
    }
  }

  /**
   * Constructs a new, empty <tt>IdentityWeakHashMap</tt> with the given initial capacity and the given load factor.
   *
   * @param initialCapacity The initial capacity of the <tt>IdentityWeakHashMap</tt>
   * @param loadFactor The load factor of the <tt>IdentityWeakHashMap</tt>
   * @throws IllegalArgumentException If the initial capacity is negative, or if the load factor is nonpositive.
   */
  public IdentityWeakHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Constructs a new, empty <tt>IdentityWeakHashMap</tt> with the given initial capacity and the default load factor,
   * which is the same as the default loadFactor or WeakHashMap.
   *
   * @param initialCapacity The initial capacity of the <tt>IdentityWeakHashMap</tt>
   * @throws IllegalArgumentException If the initial capacity is negative.
   */
  public IdentityWeakHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructs a new, empty <tt>IdentityWeakHashMap</tt> with the default initial capacity and the default load
   * factor as WeakHashMap.
   */
  public IdentityWeakHashMap() {
    super();
  }

  /**
   * Constructs a new <tt>IdentityWeakHashMap</tt> with the same mappings as the specified <tt>Map</tt>. The
   * <tt>IdentityWeakHashMap</tt> is created with default load factor, and an initial capacity defined by WeakHashMap,
   * sufficient to hold the mappings in the specified <tt>Map</tt>.
   *
   * @param m the map whose mappings are to be placed in this map.
   * @throws NullPointerException if the specified map is null.
   */
  public IdentityWeakHashMap(Map m) {
    super(m);
  }

  protected int hash(Object x) {
    int h = System.identityHashCode(x);

    h += ~(h << 9);
    h ^= (h >>> 14);
    h += (h << 4);
    h ^= (h >>> 10);
    return h;
  }

  protected boolean equal(Object obj1, Object obj2) {
    return obj1 == obj2;
  }

  protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

}