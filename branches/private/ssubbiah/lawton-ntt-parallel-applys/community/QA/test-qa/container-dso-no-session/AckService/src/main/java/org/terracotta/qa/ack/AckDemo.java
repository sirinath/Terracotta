/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.qa.ack;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.terracotta.qa.ack.AckService.Request;

public final class AckDemo {

  public static void main(String[] args) throws Exception {
    final String name = args[0];
    final Integer workerCount = new Integer(args[1]);
    final Integer clientCount = new Integer(args[2]);
    final AckService service = new AckService(name, workerCount.intValue());
    final SortedMap ackCounter = new TreeMap();
    for (int pos = 0; pos < clientCount.intValue(); ++pos) {
      Request request = new Request();
      service.ack(request, 5 * 1000);
      addAck(ackCounter, request.acknowledger());
    }
    service.stop();
    System.out.println("Requests were handled by the following:");
    for (Iterator pos = ackCounter.keySet().iterator(); pos.hasNext();) {
      String acker = pos.next().toString();
      Integer count = (Integer) ackCounter.get(acker);
      System.out.println("\t" + count + " requests handled by " + acker);
    }
  }

  private static void addAck(final Map ackCounter, final String acker) {
    Integer count = (Integer) ackCounter.get(acker);
    ackCounter.put(acker, new Integer(count == null ? 1 : count.intValue() + 1));
  }

}
