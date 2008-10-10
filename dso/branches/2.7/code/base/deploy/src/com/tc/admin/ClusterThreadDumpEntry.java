/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.util.Date;
import java.util.concurrent.Future;

public class ClusterThreadDumpEntry extends ThreadDumpTreeNode {
  private String m_text;
  
  ClusterThreadDumpEntry() {
    super(new Date());
  }

  void add(String clientAddr, Future<String> threadDump) {
    add(new ThreadDumpElement(clientAddr, threadDump));
  }

  Date getTime() {
    return (Date) getUserObject();
  }

  boolean isDone() {
    for (int i = 0; i < getChildCount(); i++) {
      ThreadDumpElement tde = (ThreadDumpElement) getChildAt(i);
      if (!tde.isDone()) return false;
    }
    return true;
  }

  void cancel() {
    for (int i = 0; i < getChildCount(); i++) {
      ThreadDumpElement tde = (ThreadDumpElement) getChildAt(i);
      if (!tde.isDone()) {
        tde.cancel();
      }
    }
  }
  
  String getContent() {
    if(m_text != null) return m_text;
    boolean isDone = isDone();
    StringBuffer sb = new StringBuffer();
    String nl = System.getProperty("line.separator");
    for (int i = 0; i < getChildCount(); i++) {
      ThreadDumpElement tde = (ThreadDumpElement) getChildAt(i);
      sb.append("---------- ");
      sb.append(tde.getSource());
      sb.append(" ----------");
      sb.append(nl);
      sb.append(nl);

      sb.append(tde.getContent());
    }
    String result = sb.toString();
    if(isDone) {
      m_text = result;
    }
    return result;
  }
}
