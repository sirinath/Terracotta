/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles.instrumentation;

import com.tc.asm.ClassVisitor;
import com.tc.object.config.schema.IncludeOnLoad;

public class ClassInstrumentationSpec {
  private final IncludeOnLoad               onLoad         = new IncludeOnLoad();
  private final RootSpec[]                  rootSpecs;
  private final DistributedMethodCallSpec[] distributedMethodSpecs;
  private final String                      className;

  private boolean                           honorTransient = false;
  private ClassVisitor                      classAdapter;
  private String[]                          transientFields;
  private LockSpec[]                        lockSpecs;

  public ClassInstrumentationSpec(String className, RootSpec[] rootSpecs,
                                  DistributedMethodCallSpec[] distributedMethodSpecs) {
    this.className = className;
    this.rootSpecs = rootSpecs;
    this.distributedMethodSpecs = distributedMethodSpecs;
  }

  public static class RootSpec {
    private final String fieldName;
    private final String rootName;

    public RootSpec(String fieldName, String rootName) {
      this.fieldName = fieldName;
      this.rootName = rootName;
    }

    public String getFieldName() {
      return fieldName;
    }

    public String getRootName() {
      return rootName;
    }
  }

  public ClassVisitor getClassAdapter() {
    return classAdapter;
  }

  public void setClassAdapter(ClassVisitor classAdapter) {
    this.classAdapter = classAdapter;
  }

  public String getClassName() {
    return className;
  }

  public DistributedMethodCallSpec[] getDistributedMethodSpecs() {
    return distributedMethodSpecs;
  }

  public RootSpec[] getRootSpecs() {
    return rootSpecs;
  }

  public boolean isHonorTransient() {
    return honorTransient;
  }

  public void setHonorTransient(boolean honorTransient) {
    this.honorTransient = honorTransient;
  }

  public LockSpec[] getLockSpecs() {
    return lockSpecs;
  }

  public void setLockSpecs(LockSpec[] lockSpecs) {
    this.lockSpecs = lockSpecs;
  }

  public String[] getTransientFields() {
    return transientFields;
  }

  public void setTransientFields(String[] transientFields) {
    this.transientFields = transientFields;
  }

  public IncludeOnLoad getOnLoad() {
    return onLoad;
  }
}
