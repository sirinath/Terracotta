/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.config.impl;

import com.tc.statistics.beans.StatisticsEmitter;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

public class StatisticsConfigImpl implements StatisticsConfig {
  private final Map defaultParams;
  private final StatisticsConfig parent;

  private Map params = new ConcurrentHashMap();

  public StatisticsConfigImpl() {
    // initialize default parameters
    Map defaultParamsMap = new HashMap();
    defaultParamsMap.put(KEY_GLOBAL_SAMPLE_PERIOD, StatisticsRetriever.DEFAULT_GLOBAL_SAMPLE_PERIOD);
    defaultParamsMap.put(KEY_EMITTER_SCHEDULE_PERIOD, StatisticsEmitter.DEFAULT_SCHEDULE_PERIOD);
    defaultParams = Collections.unmodifiableMap(defaultParamsMap);

    parent = null;
  }

  private StatisticsConfigImpl(StatisticsConfig parent) {
    Assert.assertNotNull("parent", parent);
    defaultParams = Collections.EMPTY_MAP;
    this.parent = parent;
  }

  public StatisticsConfig getParentConfig() {
    return parent;
  }

  public StatisticsConfig createNewChildConfig() {
    return new StatisticsConfigImpl(this);
  }

  public void setParam(String key, Object value) {
    params.put(key, value);
  }

  public Object getParam(String key) {
    Object value = params.get(key);
    if (null == value) {
      value = defaultParams.get(key);
    }
    if (null == value &&
        parent != null) {
      value = parent.getParam(key);
    }
    return value;
  }

  public long getParamLong(String key) {
    Object value = getParam(key);
    if (null == value) {
      value = new Long(0L);
    }
    return ((Long)value).longValue();
  }

  public String getParamString(String key) {
    Object value = getParam(key);
    if (null == value) {
      return null;
    }
    return String.valueOf(value);
  }
}
