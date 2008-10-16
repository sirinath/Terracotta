package com.tctest.perf.dashboard.common;


/**
 * 
 * Exception to be thrown in case of any errors while initializing any component
 * @author vipul
 *
 */
public class InitializationException extends Exception {

	/**
	 * 
	 */
	public InitializationException() {
	}

	/**
	 * @param message
	 */
	public InitializationException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public InitializationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InitializationException(String message, Throwable cause) {
		super(message, cause);
	}

}
