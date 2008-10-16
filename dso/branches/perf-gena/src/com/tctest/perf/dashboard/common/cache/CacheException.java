/**
 * 
 */
package com.tctest.perf.dashboard.common.cache;

public class CacheException extends Exception {

	/**
	 * 
	 */
	public CacheException() {
	}

	/**
	 * @param message
	 */
	public CacheException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public CacheException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CacheException(String message, Throwable cause) {
		super(message, cause);
	}

}
