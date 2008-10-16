/**
 * 
 */
package com.tctest.perf.dashboard.common.config;

import java.util.Properties;

import javax.naming.ConfigurationException;

/**
 * 
 * This interface declares the functionality of the configurator.
 * Any configurator (file based or DB based) should provide for the
 * methods declared in this interface
 * 
 */
public interface Configurator {

	/**
	 * Load all the properties from the source
	 * @return
	 */
	public Properties loadProperties() throws ConfigurationException;

	/**
	 * Update the given property 
	 * @param key
	 * @param value
	 */
	public void updateProperty(String key, String value) throws ConfigurationException;
	
	/**
	 * add a new Property 
	 * @param key
	 * @param value
	 */
	public void addProperty(String key, String value) throws ConfigurationException;
	
	/**
	 * Delete the given property 
	 * @param key
	 */
	public void deleteProperty(String key) throws ConfigurationException;
	
	

}
