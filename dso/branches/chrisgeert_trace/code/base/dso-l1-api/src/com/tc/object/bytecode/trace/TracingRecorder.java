/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

import bsh.EvalError;
import bsh.Interpreter;

import com.tc.asm.Opcodes;
import com.tc.logging.TCLogger;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.statistics.StatisticData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

public class TracingRecorder implements TraceListener {

  private static final TCLogger          LOGGER           = ManagerUtil.getLogger(TracingRecorder.class.getName());

  private final Interpreter              bshEngine        = new Interpreter();

  private final AtomicLong               count            = new AtomicLong();
  private final AtomicLong               totalTime        = new AtomicLong();

  private final AtomicLong               normalExits      = new AtomicLong();
  private final AtomicLong               exceptionalExits = new AtomicLong();

  private final String                   name;

  private final String                   bshOnEntry;
  private final String                   bshOnExit;
  
  private final ThreadLocal<Stack<Long>> entryTime = new ThreadLocal<Stack<Long>>() {
    protected Stack<Long> initialValue() {
      return new Stack<Long>();
    }
  };
  
  public TracingRecorder(String name) {
    this(name, null, null);
  }
  
  public TracingRecorder(String name, String bshOnEntry, String bshOnExit) {
    this.name = name;
    this.bshOnEntry = bshOnEntry;
    this.bshOnExit = bshOnExit;
  }
  
  public void methodEnter(Object self) {
    if (bshOnEntry != null) {
      try {
        bshEngine.setClassLoader(self.getClass().getClassLoader());
        bshEngine.set("self", self);
        bshEngine.eval(bshOnEntry);
      } catch (EvalError e) {
        LOGGER.error("Error executing BeanShell onEntry script", e);
      }
    }
    count.incrementAndGet();
    entryTime.get().push(Long.valueOf(System.currentTimeMillis()));
  }
  
  public void methodExit(Object self, int opcode) {
    long end = System.currentTimeMillis();
    
    try {
      long start = entryTime.get().pop().longValue();
      totalTime.addAndGet(end - start);
    } catch (EmptyStackException e) {
      LOGGER.info("More method exits than entries - listener arrived during execution?");
      return;
    }

    if (opcode == Opcodes.ATHROW) {
      exceptionalExits.incrementAndGet();
    } else {
      normalExits.incrementAndGet();
    }

    if (bshOnExit != null) {
      try {
        bshEngine.setClassLoader(self.getClass().getClassLoader());
        bshEngine.set("self", self);
        bshEngine.eval(bshOnExit);
      } catch (EvalError e) {
        LOGGER.error("Error executing BeanShell onExit script", e);
      }
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
