/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

import com.tc.asm.Opcodes;
import com.tc.statistics.StatisticData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

public class TracingRecorder implements TraceListener {

  private final AtomicLong count = new AtomicLong();
  private final AtomicLong totalTime = new AtomicLong();
  
  private final AtomicLong normalExits = new AtomicLong();
  private final AtomicLong exceptionalExits = new AtomicLong();
  
  private final String name;
  
  private final ThreadLocal<Stack<Long>> entryTime = new ThreadLocal<Stack<Long>>() {
    protected Stack<Long> initialValue() {
      return new Stack<Long>();
    }
  };
  
  public TracingRecorder(String name) {
    this.name = name;
  }
  
  public void methodEnter() {
    count.incrementAndGet();
    entryTime.get().push(Long.valueOf(System.currentTimeMillis()));
  }
  
  public void methodExit(int opcode) {
    long end = System.currentTimeMillis();
    
    if (opcode == Opcodes.ATHROW) {
      exceptionalExits.incrementAndGet();
    } else {
      normalExits.incrementAndGet();
    }
    
    try {
      long start = entryTime.get().pop().longValue();
      totalTime.addAndGet(end - start);
    } catch (EmptyStackException e) {
      System.err.println("Instrumentation Screw-Up : More Method Exits Than Entries");
    }
  }

  public Collection<StatisticData> getResults() {
    ArrayList<StatisticData> data = new ArrayList<StatisticData>();
    data.add(new StatisticData(name, "execution count", count.longValue()));
    data.add(new StatisticData(name, "total time", totalTime.longValue()));
    data.add(new StatisticData(name, "normal exits", normalExits.longValue()));
    data.add(new StatisticData(name, "exceptional exits", exceptionalExits.longValue()));
    
    return Collections.unmodifiableCollection(data);
  }

}
