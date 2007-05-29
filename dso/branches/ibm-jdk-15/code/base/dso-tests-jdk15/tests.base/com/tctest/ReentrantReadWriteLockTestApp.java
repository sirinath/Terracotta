/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCObjectNotSharableException;
import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.tx.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class ReentrantReadWriteLockTestApp extends AbstractTransparentApp {
  private final static int             NUM_OF_PUTS              = 500;

  private final CyclicBarrier          barrier, barrier2, barrier3, barrier4, barrier5;
  private final DataRoot               dataRoot                 = new DataRoot();

  private final ReentrantReadWriteLock nonFairReadWriteLockRoot = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock fairReadWriteLockRoot    = new ReentrantReadWriteLock(true);
  private ReentrantReadWriteLock       sharedReadWriteLock;
  private final Condition              nonFairCondition         = nonFairReadWriteLockRoot.writeLock().newCondition();
  private final Condition              fairCondition            = fairReadWriteLockRoot.writeLock().newCondition();

  private final List                   queue                    = new LinkedList();
  private final Random                 random;

  private int                          numOfPutters             = 1;
  private int                          numOfGetters;

  public ReentrantReadWriteLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    barrier2 = new CyclicBarrier(getParticipantCount());
    barrier3 = new CyclicBarrier(getParticipantCount());
    barrier4 = new CyclicBarrier(2);
    barrier5 = new CyclicBarrier(3);

    random = new Random(new Random(System.currentTimeMillis() + getApplicationId().hashCode()).nextLong());
    numOfGetters = getParticipantCount() - numOfPutters;
  }

  public void run() {
    try {
      int index = barrier.await();
      unsharedToSharedTest(index, new ReentrantReadWriteLock());

      readWriteLockTest(index, nonFairReadWriteLockRoot, nonFairCondition);
      ReentrantReadWriteLock localLock = new ReentrantReadWriteLock();
      singleNodeConditionVariableTesting(index, localLock.writeLock(), localLock.writeLock().newCondition());

      readWriteLockTest(index, fairReadWriteLockRoot, fairCondition);
      localLock = new ReentrantReadWriteLock(true);
      singleNodeConditionVariableTesting(index, localLock.writeLock(), localLock.writeLock().newCondition());

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void readWriteLockTest(int index, ReentrantReadWriteLock readWriteLock, Condition condition) throws Exception {
    basicTryReadLockTest(index, readWriteLock);
    basicTryReadWriteLockTest(index, readWriteLock);
    basicTryWriteReadLockTest(index, readWriteLock);
    basicTryReadLockTimeoutTest(index, readWriteLock);
    basicTryWriteLockTimeoutTest(index, readWriteLock);
    tryReadLockMultiNodeTest(index, readWriteLock);
    tryWriteLockMultiNodeTest(index, readWriteLock);
    tryReadWriteLockSingleNodeTest(index, readWriteLock);
    tryReadWriteLockSingleNodeTest(index, new ReentrantReadWriteLock());

    basicSingleNodeReadThenWriteLockingTest(index, readWriteLock);
    basicSingleNodeReadThenWriteLockingTest(index, new ReentrantReadWriteLock());
    basicMultiNodesReadThenWriteLockingTest(index, readWriteLock);
    modifyDataUsingReadLockTest(index, readWriteLock);
    modifyDataTest(index, readWriteLock);
    lockDownGradeTest(index, readWriteLock);
    reentrantReadLockWithWriteTest(index, readWriteLock);
    basicAPITest(index, readWriteLock);
    basicConditionVariableTesting(index, readWriteLock.writeLock(), condition);
    basicConditionVariableWaitTesting(index, readWriteLock, condition);
    singleNodeConditionVariableTesting(index, readWriteLock.writeLock(), condition);
  }

  private void unsharedToSharedTest(int index, ReentrantReadWriteLock lock) throws Exception {
    if (index == 1) {
      ReadLock readLock = lock.readLock();
      readLock.lock();
      try {
        dataRoot.setLock(lock);
        throw new AssertionError("Should have expected a TCObjectNotSharableException.");
      } catch (TCObjectNotSharableException e) {
        // expected
      } finally {
        readLock.unlock();
      }
      Assert.assertFalse(ManagerUtil.isManaged(lock));
    }

    barrier.await();
    
    if (index == 0) {
      WriteLock writeLock = lock.writeLock();
      writeLock.lock();
      try {
        dataRoot.setLock(lock);
        throw new AssertionError("Should have expected a TCObjectNotSharableException.");
      } catch (TCObjectNotSharableException e) {
        // expected
      } finally {
        writeLock.unlock();
      }
      Assert.assertFalse(ManagerUtil.isManaged(lock));
    }
    
    barrier.await();
  }

  private void tryReadWriteLockSingleNodeTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(2, TimeUnit.SECONDS);
            Assert.assertFalse(isLocked);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            writeLock.unlock();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t1.start();
      t2.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(4000);
      writeLock.unlock();
      localBarrier.await();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            readLock.unlock();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            writeLock.unlock();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t1.start();
      t2.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(2000);
      writeLock.unlock();
    }

    barrier.await();
  }

  private void tryWriteLockMultiNodeTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            barrier5.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            Thread.sleep(2000);
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            barrier5.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            Thread.sleep(2000);
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      writeLock.lock();
      localBarrier.await();
      barrier4.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
      barrier4.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10, TimeUnit.SECONDS);
      barrier5.await();
      Thread.sleep(2000);
      Assert.assertTrue(isLocked);
      writeLock.unlock();
      barrier4.await();
      writeLock.lock();
      Thread.sleep(2000);
      writeLock.unlock();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(4);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t3 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      t3.start();
      writeLock.lock();
      localBarrier.await();
      barrier4.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10, TimeUnit.SECONDS);
      Assert.assertTrue(isLocked);
      writeLock.unlock();
    }
    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            readLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(8, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            readLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t1.start();
      t2.start();
      writeLock.lock();
      localBarrier.await();
      barrier4.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10, TimeUnit.SECONDS);
      Assert.assertTrue(isLocked);
      writeLock.unlock();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(4, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      barrier4.await();
      Thread.sleep(1000);
      localBarrier.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10, TimeUnit.SECONDS);
      Assert.assertTrue(isLocked);
      writeLock.unlock();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            readLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      barrier4.await();
      Thread.sleep(1000);
      localBarrier.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10, TimeUnit.SECONDS);
      Assert.assertTrue(isLocked);
      writeLock.unlock();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            readLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(1000);
      barrier4.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10, TimeUnit.SECONDS);
      Assert.assertTrue(isLocked);
      writeLock.unlock();
    }

    barrier.await();

  }

  private void tryReadLockMultiNodeTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
            Assert.assertTrue(isLocked);
            readLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(1000);
      barrier2.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else {
      barrier2.await();
      writeLock.lock();
      writeLock.unlock();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            writeLock.lock();
            Thread.sleep(2001);
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(1000);
      barrier2.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock(10, TimeUnit.SECONDS);
      Assert.assertTrue(isLocked);
      readLock.unlock();
    }

    barrier.await();

    if (index == 0) {
      readLock.lock();
      try {
        barrier2.await();
        barrier2.await();
      } finally {
        readLock.unlock();
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock(1000, TimeUnit.MILLISECONDS); // We cannot use tryLock() here due to greedy
      // lock problem for tryLock().
      try {
        barrier2.await();
        Assert.assertTrue(isLocked);
      } finally {
        readLock.unlock();
      }
    }
    barrier.await();
    
    DebugUtil.DEBUG = true;
    if (DebugUtil.DEBUG) {
      System.err.println("Client id: " + ManagerUtil.getClientID() + ", index: " + index);
    }

    if (index == 0) {
      writeLock.lock();
      try {
        barrier2.await();
        barrier2.await();
      } finally {
        writeLock.unlock();
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock();
      barrier2.await();
      Assert.assertFalse(isLocked);
    }
    
    barrier.await();
    
    DebugUtil.DEBUG = false;
    
    if (index == 0) {
      writeLock.lock();
      try {
        barrier2.await();
        Thread.sleep(2000);
      } finally {
        writeLock.unlock();
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
      try {
        Assert.assertTrue(isLocked);
      } finally {
        readLock.unlock();
      }
    }
    barrier.await();

    if (index == 0) {
      writeLock.lock();
      try {
        barrier2.await();
        Thread.sleep(4000);
      } finally {
        writeLock.unlock();
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock(1, TimeUnit.SECONDS);
      Assert.assertFalse(isLocked);
    }
    barrier.await();
  }

  private void basicConditionVariableWaitTesting(int index, ReentrantReadWriteLock lock, Condition condition)
      throws Exception {
    WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      Thread thread = new Thread(new TestRunnable1(writeLock, condition));
      thread.start();
    }

    barrier.await();

    if (index == 0) {
      Thread.sleep(2000); // Sleep so that the TestRunnable1 thread can pick up.
      Assert.assertEquals(1, lock.getWaitQueueLength(condition));
      Assert.assertTrue(lock.hasWaiters(condition));
    }

    barrier.await();

    if (index == 0) {
      writeLock.lock();
      try {
        condition.signalAll();
      } finally {
        writeLock.unlock();
      }
    }

    barrier.await();

    if (index == 0) {
      Assert.assertEquals(0, lock.getWaitQueueLength(condition));
    }

    barrier.await();
  }

  private void singleNodeConditionVariableTesting(int index, final WriteLock lock, final Condition condition)
      throws Exception {
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final List queue = new LinkedList();

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            doPutter(1, lock, condition, queue, 1);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            doGetter(2, lock, condition, queue);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t1.start();
      t2.start();
      localBarrier.await();
    }

    barrier.await();
  }

  private void basicConditionVariableTesting(int index, WriteLock lock, Condition condition) throws Exception {
    final long id = new Long(getApplicationId()).longValue();

    if (index == 0) {
      doPutter(id, lock, condition, queue, numOfGetters);
    } else {
      doGetter(id, lock, condition, queue);
    }

    barrier.await();
  }

  private void doPutter(long id, WriteLock lock, Condition condition, List queue, int numOfGetters) throws Exception {
    Thread.currentThread().setName("PUTTER-" + id);

    for (int i = 0; i < NUM_OF_PUTS; i++) {
      lock.lock();
      try {
        System.err.println("PUTTER-" + id + " Putting " + i);
        queue.add(new WorkItem(String.valueOf(i)));
        if (i % 2 == 0) {
          condition.signalAll();
        } else {
          condition.signal();
        }
      } finally {
        lock.unlock();
      }
    }

    for (int i = 0; i < numOfGetters; i++) {
      lock.lock();
      try {
        queue.add(WorkItem.STOP);
        condition.signalAll();
      } finally {
        lock.unlock();
      }
    }
  }

  private void doGetter(long id, WriteLock lock, Condition condition, List queue) throws Exception {
    Thread.currentThread().setName("GETTER-" + id);

    int i = 0;
    while (true) {
      lock.lock();
      lock.lock();
      try {
        while (queue.size() == 0) {
          int choice = i % 4;
          switch (choice) {
            case 0:
              condition.await();
              break;
            case 1:
              condition.awaitUninterruptibly();
              break;
            case 2:
              long millis = random.nextInt(10000);
              condition.await(millis, TimeUnit.MILLISECONDS);
              break;
            case 3:
              long nanos = random.nextInt(10000);
              condition.awaitNanos(nanos);
              break;
          }
          i++;
        }
        WorkItem wi = (WorkItem) queue.remove(0);
        if (wi.isStop()) { return; }
        System.err.println("GETTER- " + id + " removes " + wi);

      } finally {
        lock.unlock();
        lock.unlock();
      }
    }
  }

  private void basicAPITest(int index, ReentrantReadWriteLock lock) throws Exception {
    WriteLock writeLock = lock.writeLock();
    ReadLock readLock = lock.readLock();
    if (index == 0) {
      writeLock.lock();
      writeLock.lock();
      writeLock.lock();
      barrier4.await();
      Thread.sleep(2000);
      Assert.assertTrue(lock.hasQueuedThreads());
      Assert.assertEquals(3, lock.getWriteHoldCount());
      writeLock.unlock();
      Assert.assertEquals(2, lock.getWriteHoldCount());
      writeLock.unlock();
      Assert.assertEquals(1, lock.getWriteHoldCount());
      writeLock.unlock();
      barrier4.await();
    } else if (index == 1) {
      barrier4.await();
      readLock.lock();
      readLock.lock();
      barrier4.await();
      Thread.sleep(2000);
      Assert.assertEquals(2, lock.getReadLockCount());
      readLock.unlock();
      Assert.assertEquals(1, lock.getReadLockCount());
      readLock.unlock();
    }
    barrier.await();

    Assert.assertFalse(lock.hasQueuedThreads());

    barrier.await();

    if (index == 0) {
      writeLock.lock();
      barrier2.await();
      Thread.sleep(4000);
      Assert.assertEquals(2, lock.getQueueLength());
      Assert.assertTrue(lock.isWriteLocked());
      Assert.assertTrue(lock.isWriteLockedByCurrentThread());
      writeLock.unlock();
    } else if (index == 1) {
      barrier2.await();
      readLock.lock();
      readLock.unlock();
    } else {
      barrier2.await();
      writeLock.lock();
      writeLock.unlock();
    }

    barrier.await();
  }

  private void reentrantReadLockWithWriteTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final WriteLock writeLock = lock.writeLock();
    final ReadLock readLock = lock.readLock();
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            writeLock.lock();
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      readLock.lock();
      t1.start();
      localBarrier.await();
      readLock.lock();
      readLock.unlock();
      readLock.unlock();
      localBarrier.await();
    }

    barrier.await();
  }

  private void lockDownGradeTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final WriteLock writeLock = lock.writeLock();
    final ReadLock readLock = lock.readLock();
    if (index == 0) {
      writeLock.lock();
      readLock.lock();
      writeLock.unlock();
      readLock.unlock();
    }
    barrier.await();
  }

  private void basicTryWriteLockTimeoutTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final WriteLock writeLock = lock.writeLock();
    if (index == 0) {
      boolean isLocked = writeLock.tryLock(1000, TimeUnit.SECONDS);
      try {
        barrier2.await();
        Assert.assertTrue(isLocked);
        barrier3.await();
      } finally {
        writeLock.unlock();
      }
      barrier3.await();
    } else {
      barrier2.await();
      boolean isLocked = writeLock.tryLock(5, TimeUnit.SECONDS);
      Assert.assertFalse(isLocked);
      barrier3.await();
      barrier3.await();
    }
    barrier.await();
  }

  private void basicTryReadLockTimeoutTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final ReadLock readLock = lock.readLock();
    readLock.lock();
    try {
      barrier2.await();
    } finally {
      readLock.unlock();
    }
    barrier.await();
  }

  private void modifyDataUsingReadLockTest(int index, ReentrantReadWriteLock lock) throws Exception {
    ReadLock readLock = lock.readLock();

    if (index == 0) {
      readLock.lock();
      try {
        dataRoot.putData("key1", "value1");
        throw new AssertionError("Should have thrown a ReadOnlyException.");
      } catch (ReadOnlyException re) {
        // expected;
      } finally {
        readLock.unlock();
      }
    }

    barrier.await();
  }

  private void modifyDataTest(int index, ReentrantReadWriteLock lock) throws Exception {
    ReadLock readLock = lock.readLock();
    WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      writeLock.lock();
      try {
        dataRoot.putData("key2", "value2");
      } finally {
        writeLock.unlock();
      }
      barrier2.await();
    } else {
      barrier2.await();
      readLock.lock();
      try {
        Object data = dataRoot.getData("key2");
        Assert.assertEquals("value2", data);
      } finally {
        readLock.unlock();
      }
    }

    barrier.await();
  }

  private void basicTryWriteReadLockTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      boolean isLocked = writeLock.tryLock();
      barrier2.await();
      try {
        Assert.assertTrue(isLocked);
      } finally {
        barrier3.await();
        writeLock.unlock();
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock();
      Assert.assertFalse(isLocked);
      barrier3.await();
    }

    barrier.await();
  }

  private void basicTryReadWriteLockTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      barrier2.await();
      boolean isLocked = writeLock.tryLock();
      Assert.assertFalse(isLocked);
      barrier3.await();
    } else {
      boolean isLocked = readLock.tryLock();
      try {
        barrier2.await();
        Assert.assertTrue(isLocked);
      } finally {
        barrier3.await();
        readLock.unlock();
      }
    }

    barrier.await();
  }

  private void basicTryReadLockTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final ReadLock readLock = lock.readLock();
    boolean isLocked = readLock.tryLock();
    try {
      barrier2.await();
      Assert.assertTrue(isLocked);
    } finally {
      readLock.unlock();
    }
    barrier.await();
  }

  private void basicSingleNodeReadThenWriteLockingTest(int index, ReentrantReadWriteLock lock) throws Exception {
    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(4);
      final CyclicBarrier localBarrier2 = new CyclicBarrier(5);

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          writeLock.lock();
          try {
            localBarrier.await();
            Thread.sleep(5000);
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          } finally {
            writeLock.unlock();
          }
          try {
            localBarrier2.await();
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            readLock.lock();
            try {
              Thread.sleep(2000);
            } finally {
              readLock.unlock();
            }
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
          try {
            localBarrier2.await();
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      });

      Thread t3 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            readLock.lock();
            try {
              Thread.sleep(2000);
            } finally {
              readLock.unlock();
            }
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
          try {
            localBarrier2.await();
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      });

      Thread t4 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            writeLock.lock();
            try {
              Thread.sleep(2000);
            } finally {
              writeLock.unlock();
            }
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
          try {
            localBarrier2.await();
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      });
      t1.start();
      t2.start();
      t3.start();
      t4.start();

      localBarrier2.await();
    }

    barrier.await();
  }

  private void basicMultiNodesReadThenWriteLockingTest(int index, ReentrantReadWriteLock lock) throws Exception {
    ReadLock readLock = lock.readLock();
    WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      writeLock.lock();
      try {
        barrier2.await();
        Thread.sleep(5000);
      } finally {
        writeLock.unlock();
      }
    } else if (index == 1 || index == 2) {
      barrier2.await();
      readLock.lock();
      try {
        Thread.sleep(2000);
      } finally {
        readLock.unlock();
      }
    } else if (index == 3) {
      barrier2.await();
      writeLock.lock();
      try {
        Thread.sleep(3000);
      } finally {
        writeLock.unlock();
      }
    }

    barrier.await();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ReentrantReadWriteLockTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("nonFairReadWriteLockRoot", "nonFairReadWriteLockRoot");
    spec.addRoot("fairReadWriteLockRoot", "fairReadWriteLockRoot");
    spec.addRoot("sharedReadWriteLock", "sharedReadWriteLock");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("barrier3", "barrier3");
    spec.addRoot("barrier4", "barrier4");
    spec.addRoot("barrier5", "barrier5");
    spec.addRoot("dataRoot", "dataRoot");
    spec.addRoot("nonFairCondition", "nonFairCondition");
    spec.addRoot("fairCondition", "fairCondition");
    spec.addRoot("queue", "queue");
  }

  private static class DataRoot {
    private Map                    store = new HashMap();
    private ReentrantReadWriteLock lock;

    public void putData(Object key, Object value) {
      store.put(key, value);
    }

    public Object getData(Object key) {
      return store.get(key);
    }

    public synchronized void setLock(ReentrantReadWriteLock lock) {
      this.lock = lock;
    }
  }

  private static class WorkItem {
    static final WorkItem STOP = new WorkItem("STOP");

    private final String  name;

    WorkItem(String name) {
      this.name = name;
    }

    boolean isStop() {
      return STOP.name.equals(name);
    }

    public String toString() {
      return this.name;
    }
  }

  private static class TestRunnable1 implements Runnable {
    private WriteLock lock;
    private Condition conditionObject;

    public TestRunnable1(WriteLock lock, Condition conditionObject) {
      this.lock = lock;
      this.conditionObject = conditionObject;
    }

    public void run() {
      lock.lock();
      try {
        conditionObject.await();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      } finally {
        lock.unlock();
      }
    }
  }

}
