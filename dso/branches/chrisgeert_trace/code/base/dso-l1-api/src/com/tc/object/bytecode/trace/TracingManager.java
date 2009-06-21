/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

import com.tc.logging.TCLogger;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.loaders.LoaderDescription;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.NotCompliantMBeanException;

public class TracingManager extends AbstractTerracottaMBean implements TracingManagerMBean {
  private static final TCLogger LOGGING = ManagerUtil.getLogger(TracingManager.class.getName());
  
  private TracingSRA tracingSra;
  
  public TracingManager(StatisticsAgentSubSystem statisticsAgentSubSystem) throws NotCompliantMBeanException {
    super(TracingManagerMBean.class, false);
    StatisticsRetrievalRegistry srr = statisticsAgentSubSystem.getStatisticsRetrievalRegistry();
    if (srr != null) {
      tracingSra = (TracingSRA) srr.getActionInstance(TracingSRA.ACTION_NAME);
    } else {
      LOGGING.error("The statistics retrieval registry is not available - tracing stats will not be available");
    }
  }

  public void startTracingMethodWithBeanShell(String clazz, String method, String bshOnEntry, String bshOnExit) throws Exception {
    Set<Field> fields = getTracerFields(clazz, method);

    for (Field f : fields) {
      LoaderDescription ld = ManagerUtil.getManager().getClassProvider().getLoaderDescriptionFor(f.getDeclaringClass());
      String signature = clazz + "." + method + "@" + ld.name();
      TracingRecorder tr = tracingSra.getOrCreateTracingRecorder(signature, new TracingRecorder(signature, bshOnEntry, bshOnExit));
      f.set(null, tr);
    }
  }
  
  public void startTracingMethod(String clazz, String method) throws Exception {
    startTracingMethodWithBeanShell(clazz, method, null, null);
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
    //
  }
  
  public boolean isMethodTracingEnabled() {
    return TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.METHOD_TRACING_ENABLED);
  }
}
