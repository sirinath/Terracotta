/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.IAdminClientContext;
import com.tc.admin.ServerHelper;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;

public class MethodTracingNode extends ComponentNode {

  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected MethodTracingPanel  methodTracingPanel;

  public MethodTracingNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(adminClientContext.getString("dso.method.tracing"));
    
    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    
    setIcon(ServerHelper.getHelper().getMethodTracingIcon());
  }
  
  synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public Component getComponent() {
    if (methodTracingPanel == null) {
      methodTracingPanel = new MethodTracingPanel(adminClientContext, clusterModel);
    }
    return methodTracingPanel;
  }

  @Override
  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      if (methodTracingPanel != null) {
        methodTracingPanel.tearDown();
        methodTracingPanel = null;
      }
    }
  }

}
