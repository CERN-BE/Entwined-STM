/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm.exception;

/**
 * This interface must be implemented by exceptions that may be unwrapped by the
 * {@link UnwrappableException} or {@link OasisUnavailableCheckedException}
 * .
 * 
 * @author Ivan Koblik
 * @param <T>
 *            The exception type.
 */
public interface Unwrappable<T> {
	/**
	 * Sets unwrap flag to true or false.
	 * 
	 * @param unwrappable
	 *            the unwrap flag value.
	 */
	public T setUnwrap(boolean unwrappable);

	/**
	 * Returns the unwrap flag value.
	 * 
	 * @return the unwrap flag value.
	 */
	public boolean getUnwrap();
}
