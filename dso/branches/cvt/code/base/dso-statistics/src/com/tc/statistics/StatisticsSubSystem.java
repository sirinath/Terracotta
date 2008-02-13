/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.config.schema.NewStatisticsConfig;
import com.tc.exception.TCRuntimeException;
import com.tc.statistics.beans.StatisticsEmitter;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeansNames;
import com.tc.statistics.beans.StatisticsManager;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.buffer.h2.H2StatisticsBufferImpl;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.config.impl.StatisticsConfigImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.impl.StatisticsRetrievalRegistryImpl;

import java.io.File;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

public class StatisticsSubSystem {
  private StatisticsBuffer            statisticsBuffer;
  private StatisticsEmitterMBean      statisticsEmitterMBean;
  private StatisticsManagerMBean      statisticsManagerMBean;
  private StatisticsRetrievalRegistry statisticsRetrievalRegistry;

  public void setup(final NewStatisticsConfig config) {
    StatisticsConfig globalStatisticsConfig = new StatisticsConfigImpl();
    
    // create the statistics buffer
    File statPath = config.statisticsPath().getFile();
    try {
      statPath.mkdirs();
    } catch (Exception e) {
      throw new TCRuntimeException("Unable to create the directory '"+statPath.getAbsolutePath()+"' for the statistics buffer.", e);
    }
    statisticsBuffer = new H2StatisticsBufferImpl(globalStatisticsConfig, statPath);
    try {
      statisticsBuffer.open();
    } catch (TCStatisticsBufferException sbe) {
      throw new TCRuntimeException("Unable to open the statistics buffer", sbe);
    }

    // create the statistics emitter mbean
    try {
      statisticsEmitterMBean = new StatisticsEmitter(globalStatisticsConfig, statisticsBuffer);
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the " + StatisticsEmitter.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", ncmbe);
    }

    // setup an empty statistics retrieval registry
    statisticsRetrievalRegistry = new StatisticsRetrievalRegistryImpl();
    try {
      statisticsManagerMBean = new StatisticsManager(globalStatisticsConfig, statisticsRetrievalRegistry, statisticsBuffer);
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the " + StatisticsManager.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", ncmbe);
    }
  }

  public void registerMBeans(MBeanServer mBeanServer) throws MBeanRegistrationException, NotCompliantMBeanException, InstanceAlreadyExistsException {
    mBeanServer.registerMBean(statisticsEmitterMBean, StatisticsMBeansNames.STATISTICS_EMITTER);
    mBeanServer.registerMBean(statisticsManagerMBean, StatisticsMBeansNames.STATISTICS_MANAGER);
  }

  public void unregisterMBeans(MBeanServer mBeanServer) throws InstanceNotFoundException, MBeanRegistrationException {
    mBeanServer.unregisterMBean(StatisticsMBeansNames.STATISTICS_EMITTER);
    mBeanServer.unregisterMBean(StatisticsMBeansNames.STATISTICS_MANAGER);
  }

  public void disableJMX() throws Exception {
    if (statisticsEmitterMBean != null) {
      statisticsEmitterMBean.disable();
    }
  }

  public void cleanup() throws Exception {
    statisticsBuffer.close();
  }

  public StatisticsBuffer getStatisticsBuffer() {
    return statisticsBuffer;
  }

  public StatisticsEmitterMBean getStatisticsEmitterMBean() {
    return statisticsEmitterMBean;
  }

  public StatisticsManagerMBean getStatisticsManagerMBean() {
    return statisticsManagerMBean;
  }

  public StatisticsRetrievalRegistry getStatisticsRetrievalRegistry() {
    return statisticsRetrievalRegistry;
  }
}