/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.cargo.maven2.configuration.Configuration;
import org.codehaus.cargo.maven2.configuration.Container;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

/**
 * Convert process configuration.
 * 
 * <pre>
 * &lt;processes&gt;
 *   &lt;process nodeName=&quot;master&quot; count=&quot;1&quot; jvmargs=&quot;-Xmx20m&quot;
 *     &lt;className&gt;org.terracotta.maven.plugins.tc.test.MasterProcess&lt;/className&gt;
 *   &lt;/process&gt;         
 *   &lt;process nodeName=&quot;sample&quot; count=&quot;2&quot; jvmargs=&quot;-Xmx20m&quot;
 *            className=&quot;org.terracotta.maven.plugins.tc.test.SampleProcess&quot;&gt;
 *     &lt;systemProperty key=&quot;foo&quot; value=&quot;boo&quot;/&gt;       
 *   &lt;/process&gt;         
 *   &lt;process nodeName=&quot;tomcat&quot; count=&quot;2&quot; jvmargs=&quot;-Xmx20m&quot;&gt;
 *     &lt;container&gt;
 *       &lt;containerId&gt;tomcat5x&lt;/containerId&gt;
 *       &lt;zipUrlInstaller&gt;
 *         &lt;url&gt;http://www.apache.org/dist/tomcat/tomcat-5/v5.5.25/bin/apache-tomcat-5.5.25.zip&lt;/url&gt;
 *         &lt;installDir&gt;${project.build.directory}/install/tomcat5x&lt;/installDir&gt;
 *       &lt;/zipUrlInstaller&gt;
 *     &lt;/container&gt;
 *   &lt;/process&gt;         
 * &lt;/processes&gt;
 * </pre>
 * 
 * @author Eugene Kuleshov
 */
public class ProcessConfigurationConverter extends AbstractConfigurationConverter {

  public boolean canConvert(Class type) {
    return ProcessConfiguration[].class.isAssignableFrom(type);
  }

  public Object fromConfiguration(ConverterLookup converterLookup, PlexusConfiguration configuration, Class type,
      Class baseType, ClassLoader classLoader, ExpressionEvaluator expressionEvaluator, ConfigurationListener listener)
      throws ComponentConfigurationException {
    List processes = new ArrayList();
    for (int i = 0; i < configuration.getChildCount(); i++) {
      PlexusConfiguration child = configuration.getChild(i);
      if ("process".equals(child.getName())) {
        processes.add(readProcessConfiguration("node" + i, converterLookup, child, classLoader, expressionEvaluator,
            listener));
      }

    }
    return processes.toArray(new ProcessConfiguration[processes.size()]);
  }

  private ProcessConfiguration readProcessConfiguration(String defaultNodeName, ConverterLookup converterLookup,
      PlexusConfiguration configuration, ClassLoader classLoader, ExpressionEvaluator evaluator,
      ConfigurationListener listener) throws ComponentConfigurationException {
    try {
      String nodeName = configuration.getAttribute("nodeName", defaultNodeName);
      String className = configuration.getAttribute("className", null);
      String arguments = configuration.getAttribute("arguments", null);
      String jvmArgs = configuration.getAttribute("jvmargs", null);
      String modules = configuration.getAttribute("modules", null);
      int count = Integer.parseInt(configuration.getAttribute("count", "1"));

      ProcessConfiguration process = new ProcessConfiguration();
      process.setNodeName(nodeName);
      process.setClassName(className);
      process.setArgs(arguments);
      process.setJvmArgs(jvmArgs);
      process.setModules(modules);
      process.setCount(count);
      
      for (int i = 0; i < configuration.getChildCount(); i++) {
        PlexusConfiguration child = configuration.getChild(i);
        if ("systemProperty".equals(child.getName())) {
          process.addProperty(child.getAttribute("key"), child.getAttribute("value"));
          
        } else if ("className".equals(child.getName())) {
          process.setClassName(child.getValue());
          
        } else if ("container".equals(child.getName())) {
          ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();
          Container container = (Container) converter.fromConfiguration(converterLookup, child, //
              Container.class, Container.class, classLoader, evaluator, listener);
          process.setContainer(container);
          
        } else if ("configuration".equals(child.getName())) {
          ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();
          Configuration cconfiguration = (Configuration) converter.fromConfiguration(converterLookup, child, //
              Configuration.class, Configuration.class, classLoader, evaluator, listener);
          process.setConfiguration(cconfiguration);
          
        }
      }
      
      return process;
    } catch (NumberFormatException ex) {
      throw new ComponentConfigurationException(configuration, ex);
    } catch (PlexusConfigurationException ex) {
      throw new ComponentConfigurationException(configuration, ex);
    }
  }

}
