/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.google.common.base.Predicate;

/**
 * Oasis wide facility methods. Such as argument validity check.
 * 
 * @author Ivan Koblik
 */
public class Utils {

    /**
     * A facility method that checks object to be null and throws the {@link IllegalArgumentException} if it is. This
     * method can be chained as it returns the value in case of success.
     * 
     * @param <T> Type of the checked and returned value.
     * @param name The object's name.
     * @param value The object.
     * @return The passed object.
     */
    public static <T> T checkNull(String name, T value) {
        if (null == value) {
            throw new IllegalArgumentException(name + " can't be null");
        }
        return value;
    }

    /**
     * A facility method that checks object to be null and throws the {@link IllegalArgumentException} if it is. This
     * method can be chained as it returns the value in case of success.
     * 
     * @param name The object's name.
     * @param value The object.
     * @return The passed value string in case of success.
     */
    public static String checkNull(String name, String value) {
        if (null == value) {
            throw new IllegalArgumentException(name + " can't be null");
        }
        if (value.trim().length() == 0) {
            throw new IllegalArgumentException(name + " can't be empty or be constituted only of spaces");
        }
        return value;
    }

    /**
     * A facility method that checks collection on null and emptiness and throws {@link IllegalArgumentException} if it
     * is. This method can be chained as it returns the value in case of success.
     * 
     * @param <T> The collection element type.
     * @param name The collection's name
     * @param collection The collection.
     * @return The passed collection in case of success.
     */
    public static <T> Collection<T> checkEmpty(String name, Collection<T> collection) {
        Utils.checkNull(name, collection);
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(name + " can't be empty");
        }
        return collection;
    }

    /**
     * Walks the exception cause list and checks if any of them are of the give type.
     * 
     * @param exception The exception that holds the list of causes.
     * @param causeClass The searched exception cause class type, exception itself is also checked.
     * @return true if exception or any of its causes has the required class type.
     */
    public static boolean hasCause(Throwable exception, Class<? extends Throwable> causeClass) {
        if (null == exception) {
            return false;
        }
        do {
            if (exception.getClass().equals(causeClass)) {
                return true;
            }
        } while (null != (exception = exception.getCause()));
        return false;
    }

    /**
     * A facility method that checks object to be null and throws the {@link IllegalStateException} if it is.
     * 
     * @param message The object's name.
     * @param value The object.
     */
    public static <T> T checkNullState(String message, T value) {
        if (null == value) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    /**
     * Relaxes precision up to the given power of 2.
     * <p>
     * <b>Note:</b> After applying this method you might still have digits on positions further then requested! This
     * method doesn't guarantee zeroes there, but it makes sure that if there is any difference in the original numbers
     * that is less than <i>pow(2,power)</i> it will be discarded.
     * 
     * @param value The value to round.
     * @param power Powers of 2 yielding the cutoff value.
     * @return The rounded value.
     */
    public static double round(double value, int power) {
        // Divide value by 2^power
        double scaled = Math.scalb(value, -power);
        // Discard the fractional part, multiply back by 2^power
        return Math.scalb(Math.rint(scaled), power);
    }

    /**
     * Join string representation of the elements of the given collection separated with the given delimiter.
     * 
     * @param sequence The items to be converted into string.
     * @param delimiter The delimiter to be used between distinct items of the collection.
     * @return String representations of the collection's items separated with the given delimiter.
     */
    public static <T> String join(Iterable<T> sequence, String delimiter) {
        if (null == sequence) {
            return "null";
        }
        if (null == delimiter) {
            delimiter = "";
        }
        Iterator<T> iterator = sequence.iterator();
        if (!iterator.hasNext()) {
            return "";
        }
        StringBuffer buffer = new StringBuffer(256);
        buffer.append(iterator.next());
        while (iterator.hasNext()) {
            buffer.append(delimiter);
            buffer.append(iterator.next());
        }
        return buffer.toString();
    }

    /**
     * Drains the queue to a new List<T>.
     * 
     * @param <T> The type of elements in the queue.
     * @param queue The {@link Queue} to be drained.
     * @return List of elements from the queue in the same order as they were added to the queue.
     */
    public static <T> List<T> drainQueue(Queue<T> queue) {
        Utils.checkNull("Queue", queue);

        List<T> drainedList = new LinkedList<T>();
        T newValue;
        while (null != (newValue = queue.poll())) {
            drainedList.add(newValue);
        }
        return drainedList;
    }

    /**
     * Returns the only element in the given collection.
     * 
     * @param collection The collection of elements that is expected to be of size 1.
     * @return The only element in the collection.
     * @throws IllegalStateException If the collection size is different from 1.
     */
    public static <T> T getUnique(Collection<T> collection) {
        checkNull("Collection", collection);
        if (collection.size() != 1) {
            throw new IllegalArgumentException("Collection " + collection + " is expected to contain only one element");
        }
        return collection.iterator().next();
    }

    /**
     * Predicate that accepts a string and returns <code>true</code> if given string is not null and is not composed of
     * only 0 or many spaces.
     */
    public static Predicate<String> NOT_EMPTY = new Predicate<String>() {
        @Override
        public boolean apply(String input) {
            return null != input && input.trim().length() != 0;
        }
    };

    /**
     * Prints exception stack into a string and returns it.
     * 
     * @param throwable The throwable which stack trace needs to be printed.
     * @return The stack trace.
     */
    public static String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        return stringWriter.toString();
    }
}
