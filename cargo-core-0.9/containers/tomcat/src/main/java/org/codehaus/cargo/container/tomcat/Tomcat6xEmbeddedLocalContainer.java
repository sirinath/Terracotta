/*
 * ========================================================================
 *
 * Copyright 2006 Vincent Massol.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.internal.ServletContainerCapability;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.spi.AbstractEmbeddedLocalContainer;
import org.codehaus.cargo.container.tomcat.internal.Tomcat6xEmbedded;

/**
 * Embedded Tomcat 6.x container.
 *
 * hhuynh
 */
public class Tomcat6xEmbeddedLocalContainer extends AbstractEmbeddedLocalContainer
{
    /**
     * Capability of the Tomcat/Catalina container.
     */
    private ContainerCapability capability = new ServletContainerCapability();

    /**
     * Root of the Tomcat object model.
     */
    private Tomcat6xEmbedded.Embedded controller;

    /**
     * Tomcat host object.
     */
    private Tomcat6xEmbedded.Host host;

    /**
     * {@link Deployable}s to be deployed once the container is started.
     *
     * One can only deploy to an embedded container after it's started,
     * but cargo allows you to deploy apps before the container starts.
     * so we need to remember what's supposed to be deployed.
     */
    private final List scheduledDeployables = new ArrayList();

    /**
     * Creates a Tomcat 6.x {@link org.codehaus.cargo.container.EmbeddedLocalContainer}.
     *
     * @param configuration the configuration of the newly created container.
     */
    public Tomcat6xEmbeddedLocalContainer(LocalConfiguration configuration)
    {
        super(configuration);
    }

    /**
     * @return the Tomcat controller object. Always non-null.
     */
    /*package*/ Tomcat6xEmbedded.Embedded getController()
    {
        return controller;
    }

    /**
     * @return the Tomcat host object. Always non-null.
     */
    /*package*/ Tomcat6xEmbedded.Host getHost()
    {
        return host;
    }

    /**
     * {@inheritDoc}
     * @see AbstractEmbeddedLocalContainer#doStart()
     */
    protected void doStart() throws Exception
    {
        Tomcat6xEmbedded wrapper = new Tomcat6xEmbedded(getClassLoader());

        controller = wrapper.new Embedded();

        // Tomcat always try to resolve relative path against CATALINA_BASE, so make it absolute
        // here.
        File home = new File(getConfiguration().getHome()).getAbsoluteFile();

        // NOTE: the following sets the system properties inside Tomcat (!),
        // which means you can't run two Tomcat instances inside the same VM.
        controller.setCatalinaBase(home);
        controller.setCatalinaHome(new File(getConfiguration().getHome()));

        // unless we set this explicitly, it will be loaded from CATALINA_BASE
        Tomcat6xEmbedded.MemoryRealm realm = wrapper.new MemoryRealm();
        realm.setPathname(new File(home, "conf/tomcat-users.xml"));
        controller.setRealm(realm);

        // no easy way to do this with reflection
        // controller.setLogger(new LoggerAdapter(getLogger()));

        Tomcat6xEmbedded.Engine engine = controller.createEngine();
        engine.setName("engine");
        engine.setBaseDir(home.getPath());
        engine.setParentClassLoader(getClassLoader());

        // create just one Host
        host = controller.createHost("localhost", new File(home, "webapps"));
        host.setAutoDeploy(true);
        engine.addChild(host);
        engine.setDefaultHost(host.getName());

        // publish engine
        controller.addEngine(engine);

        // create HTTP connector
        controller.addConnector(
            controller.createConnector(null, getPort(), false));

        controller.start();

        //// ideally Tomcat should be able to auto-expand a war file,
        //// but I couldn't make it work, so this is a workaround meanwhile
        if (!scheduledDeployables.isEmpty())
        {
            Deployer deployer = new Tomcat6xEmbeddedLocalDeployer(this);
            for (Iterator itr = scheduledDeployables.iterator(); itr.hasNext();)
            {
                Deployable deployable = (Deployable) itr.next();
                deployer.deploy(deployable);
            }
        }
    }

    /**
     * Tomcat's start/stop methods are synchronous, so no need for waiting.
     *
     * @param waitForStarting never used
     */
    protected void waitForCompletion(boolean waitForStarting)
    {
        // Nothing to do here as Tomcat start/stop methods are synchronous.
    }

    /**
     * Gets the port number for which this Tomcat is configured.
     *
     * @return the port number
     */
    private int getPort()
    {
        return Integer.parseInt(getConfiguration().getPropertyValue(ServletPropertySet.PORT));
    }

    /**
     * {@inheritDoc}
     * @see AbstractEmbeddedLocalContainer#doStop()
     */
    protected void doStop() throws Exception
    {
        if (controller != null)
        {
            // the stop method is allowed to be invoked multiple times
            controller.stop();
            controller = null;
            host = null;
        }
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.cargo.container.Container#getId()
     */
    public String getId()
    {
        return "tomcat6x";
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.cargo.container.Container#getName()
     */
    public String getName()
    {
        return "Tomcat 6.x Embedded";
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.cargo.container.Container#getCapability()
     */
    public ContainerCapability getCapability()
    {
        return capability;
    }

    /**
     * Used by {@link Tomcat6xEmbeddedLocalDeployer} to register {@link Deployable}s
     * that are to be deployed once the container is started.
     *
     * @param deployable
     *      {@link Deployable} to be deployed later.
     */
    void scheduleDeployment(Deployable deployable)
    {
        scheduledDeployables.add(deployable);
    }
}
