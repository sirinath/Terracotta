/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XContainer;
import com.tc.admin.model.IClient;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ClientMethodTracingPanel extends XContainer implements PropertyChangeListener {

//  private IClient client;

  public ClientMethodTracingPanel(IClient client) {
//    this.client = client;
    setName(client.toString());
    client.addPropertyChangeListener(this);
  }

  public void propertyChange(PropertyChangeEvent evt) {
    //
  }

}
