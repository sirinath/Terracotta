/* 
 * ========================================================================
 * 
 * Copyright 2004-2005 Vincent Massol.
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
package org.codehaus.cargo.container.weblogic;

import java.io.File;
import java.util.Iterator;

import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.util.FileUtils;
import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.ContainerException;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfiguration;
import org.codehaus.cargo.container.weblogic.internal.WebLogicStandaloneLocalConfigurationCapability;

/**
 * WebLogic 9x standalone
 * {@link org.codehaus.cargo.container.spi.configuration.ContainerConfiguration} implementation.
 * Written for Terracotta
 * 
 * @author hhuynh
 * @version $Id$
 */
public class WebLogic9xStandaloneLocalConfiguration extends AbstractStandaloneLocalConfiguration
{
    /**
     * Capability of the WebLogic standalone configuration.
     */
    private static ConfigurationCapability capability =
        new WebLogicStandaloneLocalConfigurationCapability();

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStandaloneLocalConfiguration#AbstractStandaloneLocalConfiguration(String)
     */
    public WebLogic9xStandaloneLocalConfiguration(String dir)
    {
        super(dir);

        setProperty(WebLogicPropertySet.ADMIN_USER, "weblogic");
        setProperty(WebLogicPropertySet.ADMIN_PWD, "weblogic");
        setProperty(WebLogicPropertySet.SERVER, "server");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.cargo.container.configuration.Configuration#getCapability()
     */
    public ConfigurationCapability getCapability()
    {
        return capability;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStandaloneLocalConfiguration#configure(LocalContainer)
     */
    protected void doConfigure(LocalContainer container) throws Exception
    {
        setupConfigurationDir();

        FilterChain filterChain = createWebLogicFilterChain();

        String[] resources =
            new String[] {"user_staged_config/readme.txt",
            "servers/AdminServer/security/boot.properties", "security/XACMLRoleMapperInit.ldift",
            "security/SerializedSystemIni.dat", "security/DefaultRoleMapperInit.ldift",
            "security/DefaultAuthenticatorInit.ldift", "lib/readme.txt",
            "init-info/tokenValue.properties", "init-info/startscript.xml",
            "init-info/security.xml", "init-info/domain-info.xml", "console-ext/readme.txt",
            "config/config.xml", "config/startup/readme.txt", "config/security/readme.txt",
            "config/nodemanager/nm_password.properties", "config/lib/readme.txt",
            "config/jms/readme.txt", "config/jdbc/readme.txt", "config/diagnostics/readme.txt",
            "config/deployments/readme.txt", "autodeploy/readme.txt", "fileRealm.properties"};

        for (int i = 0; i < resources.length; i++)
        {
            copyResource(container.getId(), resources[i], filterChain);
        }

        setupDeployables(container);
    }

    private void copyResource(String containerId, String resourceName, FilterChain filterChain)
        throws Exception
    {
        File destFile = new File(getHome(), resourceName);
        destFile.getParentFile().mkdirs();

        getResourceUtils().copyResource(RESOURCE_PATH + containerId + "/" + resourceName,
            destFile, filterChain);
    }

    /**
     * @return an Ant filter chain containing implementation for the filter tokens used in the
     *         WebLogic configuration files
     */
    private FilterChain createWebLogicFilterChain()
    {
        FilterChain filterChain = createFilterChain();

        StringBuffer appTokenValue = new StringBuffer(" ");

        Iterator it = getDeployables().iterator();
        while (it.hasNext())
        {
            Deployable deployable = (Deployable) it.next();

            if ((deployable.getType() == DeployableType.WAR)
                && ((WAR) deployable).isExpandedWar())
            {
                String context = ((WAR) deployable).getContext();
                appTokenValue.append("<Application ");
                appTokenValue.append("Name=\"_" + context + "_app\" ");
                appTokenValue.append("Path=\"" + getFileHandler().getParent(deployable.getFile())
                    + "\" ");
                appTokenValue
                    .append("StagedTargets=\"server\" StagingMode=\"stage\" TwoPhase=\"true\"");
                appTokenValue.append(">");

                appTokenValue.append("<WebAppComponent ");
                appTokenValue.append("Name=\"" + context + "\" ");
                appTokenValue.append("Targets=\"server\" ");
                appTokenValue.append("URI=\"" + context + "\"");
                appTokenValue.append("/></Application>");
            }
        }

        getAntUtils().addTokenToFilterChain(filterChain, "weblogic.apps",
            appTokenValue.toString());

        return filterChain;
    }

    /**
     * Deploy the Deployables to the weblogic configuration.
     * 
     * @param container the container to configure
     */
    protected void setupDeployables(Container container)
    {
        try
        {
            FileUtils fileUtils = FileUtils.newFileUtils();

            // Create the applications directory
            String appDir = getFileHandler().createDirectory(getHome(), "autodeploy");

            // Deploy all deployables into the applications directory
            Iterator it = getDeployables().iterator();
            while (it.hasNext())
            {
                Deployable deployable = (Deployable) it.next();
                if ((deployable.getType() == DeployableType.WAR)
                    && ((WAR) deployable).isExpandedWar())
                {
                    continue;
                }

                fileUtils.copyFile(deployable.getFile(), getFileHandler().append(appDir,
                    getFileHandler().getName(deployable.getFile())), null, true);
            }

            // Deploy the cargocpc web-app by copying the WAR file
            getResourceUtils().copyResource(RESOURCE_PATH + "cargocpc.war",
                new File(appDir, "cargocpc.war"));
        }
        catch (Exception e)
        {
            throw new ContainerException("Failed to deploy Deployables in the "
                + container.getName() + " [" + getHome() + "] domain directory", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#toString()
     */
    public String toString()
    {
        return "WebLogic Standalone Configuration";
    }
}
