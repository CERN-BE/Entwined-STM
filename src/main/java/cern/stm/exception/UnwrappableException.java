/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm.exception;

/**
 * This is a general exception for the Oasis system
 * 
 * @author S. Deghaye
 * @author Ivan Koblik
 */
public class UnwrappableException extends RuntimeException implements Unwrappable<UnwrappableException> {

    private static final long serialVersionUID = 1L;

    /**
     * Signifies behaviour of a higher level exception when passed this exception as a cause.
     */
    private boolean unwrap = true;

    /**
     * Creates an exception object with "No message" passed as exception message.
     */
    public UnwrappableException() {
        super("No message");
    }

    /**
     * Creates an exception object with the given message.
     * 
     * @param message The exception message.
     */
    public UnwrappableException(String message) {
        super(message);
    }

    /**
     * Creates an exception object with the given message and cause.
     * 
     * @param message The exception message.
     * @param cause The exception cause.
     */
    public UnwrappableException(String message, Throwable cause) {
        super(getRightMessage(message, cause), getRightException(cause));
    }

    /**
     * Creates an exception object with the given cause.
     * 
     * @param cause The exception cause.
     */
    public UnwrappableException(Throwable cause) {
        super(getRightMessage(null, cause), getRightException(cause));
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.util.exceptions.Unwrappable#getUnwrap()
     */
    public boolean getUnwrap() {
        return unwrap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.util.exceptions.Unwrappable#setUnwrap(boolean)
     */
    public UnwrappableException setUnwrap(boolean unwrap) {
        this.unwrap = unwrap;
        return this;
    }

    /**
     * Used in constructors to correctly unwrap any throwable that has to be unwrapped.
     * 
     * @param cause throwable that was thrown
     * @return same throwable, or its cause.
     */
    public static Throwable getRightException(Throwable cause) {
        if (null == cause || null == cause.getCause()) {
            return cause;
        } else if (cause instanceof Unwrappable<?>) {
            if (((Unwrappable<?>) cause).getUnwrap()) {
                return cause.getCause();
            } else if (cause.getClass().equals(cause.getCause().getClass())) {
                return cause.getCause();
            }
        }
        return cause;
    }

    /**
     * Used in constructors to correctly unwrap any message from cause. Messages always pile up.
     * 
     * @param cause throwable that was thrown
     * @return throwable's message.
     */
    public static String getRightMessage(String message, Throwable cause) {
        String oldMessage = null;
        if (null == cause) {
            return message;
        } else {
            if (null == (oldMessage = cause.getMessage())) {
                oldMessage = (null == cause.getCause() ? oldMessage : cause.getCause().getMessage());
            }
        }
        String fullMessage = message;
        if (oldMessage != null) {
            fullMessage = (null != fullMessage ? fullMessage + ", " + oldMessage : oldMessage);
        } else {
            fullMessage = (null != fullMessage ? fullMessage + ", " + cause.toString() : cause.toString());
        }
        return fullMessage;
    }
}
