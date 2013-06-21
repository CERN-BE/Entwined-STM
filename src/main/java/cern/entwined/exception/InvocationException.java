/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined.exception;

/**
 * Generic exception that is used to wrap checked exceptions in user code.
 * 
 * @author Ivan Koblik
 */
public class InvocationException extends UnwrappableException {

	/**
	 * Exception version id.
	 */
	private static final long serialVersionUID = -4855258409489444927L;

	/**
	 * @see UnwrappableException#OasisUnavailableException()
	 */
	public InvocationException() {
		super();
		this.setUnwrap(true);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(String)
	 */
	public InvocationException(String message) {
		super(message);
		this.setUnwrap(true);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(String, Throwable)
	 */
	public InvocationException(String message, Throwable cause) {
		super(message, cause);
		this.setUnwrap(true);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(Throwable)
	 */
	public InvocationException(Throwable cause) {
		super(cause);
		this.setUnwrap(true);
	}
}
