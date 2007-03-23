/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.eclipse.swt.SWT;

public interface XmlObjectHolder {
  final String RESET        = "Reset";
  final int    RESET_STROKE = SWT.CONTROL + 'g';

  boolean isRequired();

  boolean isSet();

  void set();

  void unset();
}
