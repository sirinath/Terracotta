/*
 * ========================================================================
 *
 * Copyright 2003-2008 The Apache Software Foundation. Code from this file 
 * was originally imported from the Jakarta Cactus project.
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ========================================================================
 */
package org.codehaus.cargo.container.tomcat;

import java.io.File;

import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.tomcat.internal.AbstractCatalinaInstalledLocalContainer;

/**
 * Special container support for the Apache Tomcat 7.x servlet container.
 * 
 * @version $Id: Tomcat7xInstalledLocalContainer.java 2760 2011-03-19 17:33:47Z
 *          alitokmen $
 */
public class Tomcat7xInstalledLocalContainer extends AbstractCatalinaInstalledLocalContainer {
  /**
   * Unique container id.
   */
  public static final String ID = "tomcat7x";

  /**
   * {@inheritDoc}
   * 
   * @see AbstractCatalinaInstalledLocalContainer#AbstractCatalinaInstalledLocalContainer(org.codehaus.cargo.container.configuration.LocalConfiguration)
   */
  public Tomcat7xInstalledLocalContainer(LocalConfiguration configuration) {
    super(configuration);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.codehaus.cargo.container.Container#getId()
   */
  public final String getId() {
    return ID;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.codehaus.cargo.container.Container#getName()
   */
  public final String getName() {
    return "Tomcat " + getVersion("7.x");
  }

  @Override
  protected void invokeContainer(String action, Java java) throws Exception {
    Path cp = java.createClasspath();
    cp.createPathElement().setPath(new File(this.getHome(), "bin/tomcat-juli.jar").getAbsolutePath());
    super.invokeContainer(action, java);
  }
}
