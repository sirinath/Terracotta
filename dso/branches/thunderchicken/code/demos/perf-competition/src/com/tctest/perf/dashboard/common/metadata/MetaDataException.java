/**
 * 
 */
package com.tctest.perf.dashboard.common.metadata;

public class MetaDataException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7686267667029580305L;

	/**
	 * 
	 */
	public MetaDataException() {
	}

	/**
	 * @param message
	 */
	public MetaDataException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public MetaDataException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MetaDataException(String message, Throwable cause) {
		super(message, cause);
	}

}
