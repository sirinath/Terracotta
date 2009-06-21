/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class TracingSRA implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "tracing data";
  
  private final ConcurrentHashMap<String, TracingRecorder> tracings = new ConcurrentHashMap<String, TracingRecorder>();
  
  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public TracingRecorder getOrCreateTracingRecorder(String methodSignature, TracingRecorder tr) {
    TracingRecorder installed = tracings.get(methodSignature);
    if (installed == null) {
      TracingRecorder old = tracings.putIfAbsent(methodSignature, tr);
      if (old != null) {
        installed = old;
      } else {
        installed = tr;
      }
    }
    
    return installed;
  }
  
  public void removeTracingRecorder(String methodSignature) {
    tracings.remove(methodSignature);
  }

  public StatisticData[] retrieveStatisticData() {
    ArrayList<StatisticData> results = new ArrayList<StatisticData>(tracings.size());
    for (TracingRecorder tr : tracings.values()) {
      results.addAll(tr.getResults());
    }
    return results.toArray(new StatisticData[results.size()]);
  }
}
