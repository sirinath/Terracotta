/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.terracottatech.config.DsoClientData;

/**
 * The standard implementation of {@link DSOInstrumentationLoggingOptions}.
 */
public class StandardDSOInstrumentationLoggingOptions extends BaseNewConfigObject implements
    DSOInstrumentationLoggingOptions {

  private final BooleanConfigItem logClass;
  private final BooleanConfigItem logLocks;
  private final BooleanConfigItem logTransientRoot;
  private final BooleanConfigItem logRoots;
  private final BooleanConfigItem logDistributedMethods;

  public StandardDSOInstrumentationLoggingOptions(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(DsoClientData.class);

    this.logClass = this.context.booleanItem("debugging/instrumentation-logging/class");
    this.logLocks = this.context.booleanItem("debugging/instrumentation-logging/locks");
    this.logTransientRoot = this.context.booleanItem("debugging/instrumentation-logging/transient-root");
    this.logRoots = this.context.booleanItem("debugging/instrumentation-logging/roots");
    this.logDistributedMethods = this.context.booleanItem("debugging/instrumentation-logging/distributed-methods");
  }

  public BooleanConfigItem logClass() {
    return this.logClass;
  }

  public BooleanConfigItem logLocks() {
    return this.logLocks;
  }

  public BooleanConfigItem logTransientRoot() {
    return this.logTransientRoot;
  }

  public BooleanConfigItem logRoots() {
    return this.logRoots;
  }

  public BooleanConfigItem logDistributedMethods() {
    return this.logDistributedMethods;
  }

}
