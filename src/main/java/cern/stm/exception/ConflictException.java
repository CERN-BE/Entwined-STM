/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm.exception;

/**
 * Exception is thrown by transactional data types at commit time when a
 * conflict is detected.
 * 
 * @author Ivan Koblik
 */
public class ConflictException extends MemoryException {

	/**
	 * Exception version id.
	 */
	private static final long serialVersionUID = -4855258409489444926L;

	/**
	 * @see UnwrappableException#OasisUnavailableException()
	 */
	public ConflictException() {
		super();
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(String)
	 */
	public ConflictException(String message) {
		super(message);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(String, Throwable)
	 */
	public ConflictException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(Throwable)
	 */
	public ConflictException(Throwable cause) {
		super(cause);
	}
}
