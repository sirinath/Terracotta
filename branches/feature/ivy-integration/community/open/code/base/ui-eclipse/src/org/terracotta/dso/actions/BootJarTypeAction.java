/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IType;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.ConfigurationEditor;

/**
 * This is currently not used.
 */

/**
 * Marks the currently selected IType as a BootJar class.
 * 
 * @see org.eclipse.jdt.core.IType
 * @see org.terracotta.dso.ConfigurationHelper.isBootJarClass
 * @see org.terracotta.dso.ConfigurationHelper.ensureBootJarClass
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotBootJarClass
 */

public class BootJarTypeAction extends BaseAction {
  private IType m_type;
  
  public BootJarTypeAction() {
    super("Boot Jar", AS_CHECK_BOX);
  }
  
  public void setType(IType type) {
    setJavaElement(m_type = type);
    
    boolean isBootClass = TcPlugin.getDefault().isBootClass(type); 
    setEnabled(!isBootClass);
    setChecked(isBootClass || getConfigHelper().isBootJarClass(type));
  }
  
  public void performAction() {
    ConfigurationHelper helper = getConfigHelper();
    
    if(isChecked()) {
      helper.ensureBootJarClass(m_type);
    }
    else {
      helper.ensureNotBootJarClass(m_type);
    }

    TcPlugin            plugin = TcPlugin.getDefault();
    ConfigurationEditor editor = plugin.getConfigurationEditor(getProject());

    if(editor != null) {
      editor.modelChanged();
    }

    inspectCompilationUnit();
  }
}
