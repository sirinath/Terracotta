/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class TCSocketAddressTest extends TestCase {

  public void testHashCode() {
    TCSocketAddress tsa1 = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 9000);
    TCSocketAddress tsa2 = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 9000);

    assertTrue(tsa1.hashCode() == tsa2.hashCode());
  }

  public void testPortRanges() {
    TCSocketAddress s1 = new TCSocketAddress(0);
    TCSocketAddress s2 = new TCSocketAddress(65535);
    if (s1 != null) s1 = null; // silence warning
    if (s2 != null) s2 = null; // silence warning

    boolean exception = false;

    try {
      TCSocketAddress s3 = new TCSocketAddress(-1);
      if (s3 != null) s3 = null; // silence warning
    } catch (IllegalArgumentException e) {
      exception = true;
    }
    assertTrue(exception);

    exception = false;
    try {
      TCSocketAddress s3 = new TCSocketAddress(65536);
      if (s3 != null) s3 = null; // silence warning
    } catch (IllegalArgumentException e) {
      exception = true;
    }
    assertTrue(exception);
  }

  public void testStringForm() {
    TCSocketAddress sa = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 12345);
    String s = sa.getStringForm();
    assertTrue(s.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+\\:12345$"));
  }

  public void testEquals() {
    TCSocketAddress tsa1 = new TCSocketAddress(9000);
    TCSocketAddress tsa2 = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 9000);
    TCSocketAddress tsa3 = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 9000);

    assertTrue(tsa1.equals(tsa2));
    assertTrue(tsa2.equals(tsa3));
  }

  public void testInCollections() throws UnknownHostException {
    TCSocketAddress tsa1 = new TCSocketAddress(9000);
    TCSocketAddress tsa2 = new TCSocketAddress(InetAddress.getByName("1.2.3.4"), 9000);
    TCSocketAddress tsa3 = new TCSocketAddress(InetAddress.getByName("1.2.3.4"), 9000);

    Map map = new HashMap();

    Object val1 = new Object();
    Object val2 = new Object();
    Object val3 = new Object();

    map.put(tsa1, val1);
    map.put(tsa2, val2);
    map.put(tsa3, val3);

    assertTrue(map.size() == 2);

    assertTrue(map.get(tsa1) == val1);
    assertTrue(map.get(tsa2) == val3);
    assertTrue(map.get(tsa3) == val3);
  }
}