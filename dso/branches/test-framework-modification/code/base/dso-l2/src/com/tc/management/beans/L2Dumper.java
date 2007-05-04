/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.AbstractTerracottaMBean;

import java.lang.reflect.Method;

import javax.management.NotCompliantMBeanException;

public class L2Dumper extends AbstractTerracottaMBean implements L2DumperMBean {
  public static final String  THREAD_DUMP_METHOD_NAME       = "dumpThreadsMany";
  public static final Class[] THREAD_DUMP_METHOD_PARAMETERS = new Class[] { int.class, long.class };
  public static final int     THREAD_DUMP_COUNT             = 3;
  public static final long    THREAD_DUMP_DELAY             = 1000;

  private final TCDumper      dumper;

  public L2Dumper(TCDumper dumper) throws NotCompliantMBeanException {
    super(L2DumperMBean.class, false);
    this.dumper = dumper;
  }

  public void doServerDump() {
    dumper.dump();
  }

  public void doThreadDump() throws Exception {
    Class threadDumpClass = getClass().getClassLoader().loadClass("com.tc.util.runtime.ThreadDump");
    Method method = threadDumpClass.getMethod(THREAD_DUMP_METHOD_NAME, THREAD_DUMP_METHOD_PARAMETERS);
    Object[] args = { new Integer(THREAD_DUMP_COUNT), new Long(THREAD_DUMP_DELAY) };
    method.invoke(null, args);
  }

  public void reset() {
    //
  }

}
