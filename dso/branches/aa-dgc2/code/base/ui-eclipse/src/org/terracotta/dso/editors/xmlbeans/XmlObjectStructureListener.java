/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import java.util.EventListener;

public interface XmlObjectStructureListener extends EventListener {
  void structureChanged(XmlObjectStructureChangeEvent e);
}
