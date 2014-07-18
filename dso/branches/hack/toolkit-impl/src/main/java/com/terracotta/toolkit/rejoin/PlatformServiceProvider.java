/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.rejoin;

import com.tc.platform.PlatformService;

public class PlatformServiceProvider {
  private static volatile PlatformService platformService = null;

  public synchronized static void setPlatformService(PlatformService service) {
    platformService = service;
  }

  public static PlatformService getPlatformService() {
    return platformService;
  }
}
