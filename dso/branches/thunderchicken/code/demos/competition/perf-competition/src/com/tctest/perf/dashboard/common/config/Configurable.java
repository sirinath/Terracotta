/**
 * 
 */
package com.tctest.perf.dashboard.common.config;


/**
 * 
 * Any entity that needs to be configured in the entire system 
 * and intends to use the 'Configuration' should implement this interface  
 *
 */
public interface Configurable {

	/**
	 * every configurable should provide a unique namespace
	 * @return
	 */
	public String getNamespace();
	
}
