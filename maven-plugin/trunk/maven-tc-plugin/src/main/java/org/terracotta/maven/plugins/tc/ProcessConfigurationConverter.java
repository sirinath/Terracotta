/*
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

/**
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
        processes.add(readProcessConfiguration(child, expressionEvaluator, "node" + i));
      }

    }
    return processes.toArray(new ProcessConfiguration[processes.size()]);
  }

  private ProcessConfiguration readProcessConfiguration(PlexusConfiguration configuration,
      ExpressionEvaluator evaluator, String defaultNodeName) throws ComponentConfigurationException {
    try {
      String nodeName = configuration.getAttribute("nodeName", defaultNodeName);
      String className = configuration.getAttribute("className");
      String arguments = configuration.getAttribute("arguments", null);
      String jvmArgs = configuration.getAttribute("jvmargs", null);
      int count = Integer.parseInt(configuration.getAttribute("count", "1"));

      Map systemProperties = new HashMap();
      for (int i = 0; i < configuration.getChildCount(); i++) {
        PlexusConfiguration child = configuration.getChild(i);
        if ("systemProperty".equals(child.getName())) {
          systemProperties.put(child.getAttribute("key"), child.getAttribute("value"));
        }
      }

      return new ProcessConfiguration(nodeName, className, arguments, jvmArgs, systemProperties, count);
    } catch (NumberFormatException ex) {
      throw new ComponentConfigurationException(configuration, ex);
    } catch (PlexusConfigurationException ex) {
      throw new ComponentConfigurationException(configuration, ex);
    }
  }

}
