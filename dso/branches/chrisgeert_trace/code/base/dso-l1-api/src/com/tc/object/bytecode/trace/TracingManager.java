/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.loaders.LoaderDescription;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.NotCompliantMBeanException;

public class TracingManager extends AbstractTerracottaMBean implements TracingManagerMBean {

  private final TracingSRA tracingSra;
  
  public TracingManager(StatisticsAgentSubSystem statisticsAgentSubSystem) throws NotCompliantMBeanException {
    super(TracingManagerMBean.class, true);
    StatisticsRetrievalRegistry srr = statisticsAgentSubSystem.getStatisticsRetrievalRegistry();
    tracingSra = (TracingSRA) srr.getActionInstance(TracingSRA.ACTION_NAME);
  }

  public void startTracingMethod(String clazz, String method) throws Exception {
    try {
      Set<Field> fields = getTracerFields(clazz, method);

      for (Field f : fields) {
        LoaderDescription ld = ManagerUtil.getManager().getClassProvider().getLoaderDescriptionFor(f.getDeclaringClass());
        TracingRecorder tr = tracingSra.getOrCreateTracingRecorder(clazz + "." + method + "@" + ld.name());
        f.set(null, tr);
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public void stopTracingMethod(String clazz, String method) throws Exception {
    for (Field f : getTracerFields(clazz, method)) {
      LoaderDescription ld = ManagerUtil.getManager().getClassProvider().getLoaderDescriptionFor(f.getDeclaringClass());
        tracingSra.removeTracingRecorder(clazz + "." + method + "@" + ld.name());
        f.set(null, null);
    }
  }
  
  private static Set<Field> getTracerFields(String clazz, String method) throws Exception {
    Set<Class> clazzes = getClasses(clazz);
    String tracerFieldName = getTracerFieldName(method);
    
    Set<Field> fields = new HashSet();
    for (Class c : clazzes) {
      Field tracerField = c.getDeclaredField(tracerFieldName);
      tracerField.setAccessible(true);
      fields.add(tracerField);
    }
    
    return Collections.unmodifiableSet(fields);
  }

  public static String getTracerFieldName(String method) {
    return "__tc_trace_" + method.replaceAll("[^\\p{javaJavaIdentifierPart}]", "_");
  }

  private static Set<Class> getClasses(String clazz) {
    return ManagerUtil.getManager().getClassProvider().getLoadedClasses(clazz);
  }

  public void reset() {
    // What should this do?
  }
}
