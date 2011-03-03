/**
 * 
 */
package com.tctest.perf.dashboard.stats.app.test;

import java.util.Properties;

import javax.naming.ConfigurationException;

import com.tctest.perf.dashboard.common.config.Configurator;

public class StressTestConfigurator implements Configurator {

	/**
	 * properties for storing the configuration parameters
	 */
	Properties properties = new Properties();

	/**
	 * 
	 */
	public void addProperty(String key, String value)
			throws ConfigurationException {
		if (properties.containsKey(key)) {
			throw new ConfigurationException("Property with key "+key+" already available.");
		}
		properties.put(key, value);
	}

	/**
	 * 
	 */
	public void deleteProperty(String key) throws ConfigurationException {
		if (!properties.containsKey(key)) {
			throw new ConfigurationException("Property with key "+key+" not found.");
		}
		properties.remove(key);
		
	}

	/**
	 * 
	 */
	public Properties loadProperties() throws ConfigurationException {
		return properties;
	}

	/**
	 * 
	 */
	public void updateProperty(String key, String value)
			throws ConfigurationException {
		if (!properties.containsKey(key)) {
			throw new ConfigurationException("Property with key "+key+" not found.");
		}
		properties.put(key, value);
	}

	
	
 
	
	{//APP INGESTOR PROPERTIES
 
		properties.put("app.stats.metadata.tree.depth", "5");
		properties.put("app.stats.metadata.stats.count", "180");
		
		//NODES
		int nodeCount = 0;
		for(int appId = 1;appId<=5;appId++){ //5 applications
			for(int dcId = 1;dcId<=2;dcId++){ //2 dcs each (total 10)
				for(int podId = 1;podId<=6;podId++){ //6 partitions each (total 60)
					for(int hostId = 1;hostId<=20;hostId++){ //20 hosts each (total 1200)
						for(int instanceId = 1;instanceId<=6;instanceId++){ //6 instances each (total 7200)
							StringBuffer b = new StringBuffer();
							b = b.append("APP-").append(appId);
							b = b.append(".").append("DC-").append(dcId);
							b = b.append(".").append("POD-").append(podId);
							b = b.append(".").append("HOST-").append(hostId);
							b = b.append(".").append("INSTANCE-").append(instanceId);
							String property = "app.stats.metadata.tree.root."+b.toString();
							System.out.println("adding node "+property);
							properties.put(property, "Y");
							nodeCount ++;
						}
					}
				}
			}
		}
		
		System.out.println("************** added "+nodeCount + " nodes");
		System.out.println();
		for(int appId = 1;appId<=5;appId++){ //5 applications
			for(int eventId = 1 ; eventId <=5;eventId++){
				String property = "app.stats.metadata.events.eventname.Event_"+appId+"_"+eventId+".tree.root.APP-"+appId;
				System.out.println("adding event "+property);
				properties.put(property, "Y");
			}
		}
		
	}
}
