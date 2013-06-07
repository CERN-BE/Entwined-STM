/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm.exception;

/**
 * Exception is thrown when a transactional data is attempted to be accessed
 * outside of a transaction.
 * 
 * @author Ivan Koblik
 */
public class NoTransactionException extends MemoryException {

	/**
	 * Exception version id.
	 */
	private static final long serialVersionUID = 1232134715126838087L;

	/**
	 * @see UnwrappableException#OasisUnavailableException()
	 */
	public NoTransactionException() {
		super();
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(String)
	 */
	public NoTransactionException(String message) {
		super(message);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(String, Throwable)
	 */
	public NoTransactionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @see UnwrappableException#OasisUnavailableException(Throwable)
	 */
	public NoTransactionException(Throwable cause) {
		super(cause);
	}

}
