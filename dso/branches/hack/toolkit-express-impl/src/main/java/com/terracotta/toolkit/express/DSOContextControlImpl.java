/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.object.bytecode.hook.DSOContext;

import java.util.Set;

public class DSOContextControlImpl implements DSOContextControl {

  private final DSOContext dsoContext;

  public DSOContextControlImpl(Object context) {
    this.dsoContext = (DSOContext) context;
  }

  @Override
  public void activateTunnelledMBeanDomains(Set<String> tunnelledMBeanDomains) {
    boolean sendCurrentTunnelledDomains = false;
    if (tunnelledMBeanDomains != null) {
      for (String mbeanDomain : tunnelledMBeanDomains) {
        dsoContext.addTunneledMBeanDomain(mbeanDomain);
        sendCurrentTunnelledDomains = true;
      }
    }
    if (sendCurrentTunnelledDomains) {
      dsoContext.sendCurrentTunneledDomains();
    }
  }

  @Override
  public void shutdown() {
    dsoContext.shutdown();
  }

  @Override
  public boolean isOnline() {
    return dsoContext.getPlatformService().getDsoCluster().areOperationsEnabled();
  }

  @Override
  public Object getPlatformService() {
    return dsoContext.getPlatformService();
  }

  @Override
  public void init() {
    try {
      dsoContext.init();
    } catch (ConfigurationSetupException e) {
      throw new RuntimeException(e);
    }
  }
}
