/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

/**
 * A generic reference interface, can hold only one value at time.
 * 
 * @param <T> The referenced value type.
 * @author Ivan Koblik
 */
public interface Ref<T> {

    /**
     * Returns kept value.
     * 
     * @return The referenced value.
     */
    public T deref();

    /**
     * Associates this reference with another value.
     * 
     * @param value The new value.
     * @return The old referenced value.
     */
    public T assoc(T value);
}
