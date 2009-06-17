/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.modules;

import java.io.File;

public class RepositoryInfo extends ModuleInfoGroup {
  private File repositoryDir;
  
  public File getRepositoryDir() {
    return repositoryDir;
  }

  public RepositoryInfo(File repositoryDir) {
    this.repositoryDir = repositoryDir;
  }
}
