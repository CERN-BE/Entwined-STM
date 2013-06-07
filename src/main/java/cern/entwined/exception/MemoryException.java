/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined.exception;

/**
 * Generic exception that may be thrown by the transactional memory management
 * code.
 * <p>
 * Memory exceptions cannot be unwrapped.
 * 
 * @author Ivan Koblik
 */
public class MemoryException extends UnwrappableException {

	/**
	 * Exception version id.
	 */
	private static final long serialVersionUID = -4855258409489444927L;

	/**
	 * @see UnwrappableException#OasisUnavailableException()
	 */
	public MemoryException() {
		super();
		this.setUnwrap(false);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(String)
	 */
	public MemoryException(String message) {
		super(message);
		this.setUnwrap(false);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(String, Throwable)
	 */
	public MemoryException(String message, Throwable cause) {
		super(message, cause);
		this.setUnwrap(false);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(Throwable)
	 */
	public MemoryException(Throwable cause) {
		super(cause);
		this.setUnwrap(false);
	}

}
