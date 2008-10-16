/**
 * 
 */
package com.tctest.perf.dashboard.common.data;

public class DataException extends Exception {

	/**
	 * 
	 */
	public DataException() {
 	}

	/**
	 * @param message
	 */
	public DataException(String message) {
		super(message);
 	}

	/**
	 * @param cause
	 */
	public DataException(Throwable cause) {
		super(cause);
 	}

	/**
	 * @param message
	 * @param cause
	 */
	public DataException(String message, Throwable cause) {
		super(message, cause);
 	}

}
