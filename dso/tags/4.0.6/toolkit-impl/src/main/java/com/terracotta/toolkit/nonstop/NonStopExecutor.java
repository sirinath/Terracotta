/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NonStopExecutor {
  public final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
      1,
      new ThreadFactory() {
        private final AtomicLong threadNumber = new AtomicLong();

        @Override
        public Thread newThread(final Runnable r) {
          Thread thread = new Thread(r, "NonStopExecutor thread-" + threadNumber
              .getAndIncrement());
          thread.setDaemon(true);
          return thread;
        }
      });

  public Future schedule(Runnable task, long timeout) {
    return executor.schedule(task, timeout, TimeUnit.MILLISECONDS);
  }

  public void remove(Future future) {
    if (future instanceof Runnable) {
      executor.remove((Runnable) future);
    }
  }

  public void shutdown() {
    executor.shutdown();
  }

}
