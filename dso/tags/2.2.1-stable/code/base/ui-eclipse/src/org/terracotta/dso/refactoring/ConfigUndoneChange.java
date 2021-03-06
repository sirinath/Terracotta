/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.terracotta.dso.TcPlugin;
import com.terracottatech.configV2.TcConfigDocument.TcConfig;

public class ConfigUndoneChange extends Change {
  private TcConfig fConfig;
  private IProject fProject;
    
  public ConfigUndoneChange(IProject project, TcConfig config) {
    super();
    fProject = project;
    fConfig  = config;
  }
  
  public Object getModifiedElement() {
    return null;
  }
  
  public String getName() {
    return "TCConfigUndoneUpdate";
  }
  
  public void initializeValidationData(IProgressMonitor pm) {/**/}
  
  public RefactoringStatus isValid(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }
  
  public Change perform(IProgressMonitor pm) {
    TcPlugin plugin = TcPlugin.getDefault();
    TcConfig config = (TcConfig)plugin.getConfiguration(fProject).copy();
    
    try {
      plugin.setConfigurationFromString(fProject, fConfig.xmlText(plugin.getXmlOptions()));
    } catch(Exception e) {
      e.printStackTrace();
    }
    
    // create the undo change
    return new ConfigUndoneChange(fProject, config);
  }
}
