/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.trace;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XObjectTableModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class MethodTracingTableModel extends XObjectTableModel {
  private static final String[]            cFields     = { "Name", "Requested", "Hops", "Waiters", "AcquireTime",
      "HeldTime"                                      };
  private final String[]                   cTips;

  public MethodTracingTableModel(ApplicationContext appContext) {
    super(MethodTraceResultWrapper.class, cFields, (String[]) appContext.getObject("dso.locks.column.headings"));
    cTips = (String[]) appContext.getObject("dso.locks.column.tips");
  }

  public MethodTracingTableModel(ApplicationContext appContext, Collection<MethodTraceResult> traceResults) {
    this(appContext);
    ArrayList list = new ArrayList<MethodTraceResultWrapper>();
    Iterator<MethodTraceResult> iter = traceResults.iterator();
    while (iter.hasNext()) {
      list.add(new MethodTraceResultWrapper(iter.next()));
    }
    Collections.sort(list, new Comparator<MethodTraceResultWrapper>() {
      public int compare(MethodTraceResultWrapper o1, MethodTraceResultWrapper o2) {
        return o1.getMethodSignature().compareTo(o2.getMethodSignature());
      }
    });
    add(list);
  }

  public void notifyChanged() {
    fireTableDataChanged();
  }

  public String columnTip(int column) {
    return cTips[column];
  }

  public static class MethodTraceResultWrapper {
    private MethodTraceResult traceResult;

    MethodTraceResultWrapper(MethodTraceResult traceResult) {
      this.traceResult = traceResult;
    }

    public String getMethodSignature() {
      return traceResult.getMethodSignature();
    }

    public long getExecutionCount() {
      return traceResult.getExecutionCount();
    }

    public long getNormalReturnCount() {
      return traceResult.getExceptionalReturnCount();
    }

    public long getExceptionalReturnCount() {
      return traceResult.getExceptionalReturnCount();
    }

    public float getMeanExecutionTime() {
      return (1f * traceResult.getTotalExecutionTime()) / traceResult.getExecutionCount();
    }
  }
}
