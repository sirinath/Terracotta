/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.util.TIMUtil;

public class EhcacheGlobalEviction130Test extends EhcacheGlobalEvictionTestBase {

  public EhcacheGlobalEviction130Test() {
    // disableAllUntil("2008-01-15");
  }

  protected Class getApplicationClass() {
    return EhcacheGlobalEviction130TestApp.class;
  }

  protected String getEhcacheVersion() {
    return TIMUtil.EHCACHE_1_3;
  }

}
