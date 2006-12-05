/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.qa.ack;

import java.util.LinkedList;

public final class AckService {

  public static class Request {

    private String acknowledger;

    public void ack(String acknowledger) {
      synchronized (this) {
        this.acknowledger = acknowledger;
        notifyAll();
      }
    }

    public void waitForAck(long timeout) {
      final long stopTime = timeout > 0 ? System.currentTimeMillis() + timeout : Long.MAX_VALUE;
      synchronized (this) {
        while (System.currentTimeMillis() < stopTime && acknowledger() == null) {
          try {
            wait(Math.abs(stopTime - System.currentTimeMillis()));
          } catch (InterruptedException ie) {
            // Ignore
          }
        }
      }
    }

    public synchronized String acknowledger() {
      return acknowledger;
    }
  }

  private final LinkedList queue;

  private volatile boolean run;

  public AckService(final String name, final int workerCount) {
    queue = new LinkedList();
    run = true;
    for (int pos = 0; pos < workerCount; ++pos) {
      new AckProcessor(name + "-" + pos).start();
    }
  }

  public void ack(Request request, long timeout) throws InterruptedException {
    if (request != null) {
      synchronized (queue) {
        queue.addLast(request);
        queue.notifyAll();
      }
      request.waitForAck(timeout);
    }
  }

  public void stop() {
    run = false;
    synchronized (queue) {
      queue.notifyAll();
    }
  }

  public static void main(String[] args) throws Exception {
    new AckService(args[0], Integer.parseInt(args[1]));
    synchronized (args) {
      args.wait();
    }
  }

  private class AckProcessor extends Thread {

    private final String myID;

    AckProcessor(final String id) {
      myID = id;
    }

    public void run() {
      while (run) {
        Request request = null;
        synchronized (queue) {
          if (!queue.isEmpty()) {
            request = (Request) queue.removeFirst();
          } else {
            try {
              queue.wait();
            } catch (InterruptedException ie) {
              // Ignore
            }
          }
        }
        if (request != null) {
          request.ack(myID);
        }
      }
    }
  }
}
