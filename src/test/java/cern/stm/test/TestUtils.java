/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm.test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

import cern.stm.Utils;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Oasis test utilities.
 * 
 * @author Ivan Koblik
 */
public class TestUtils {

	/**
	 * Common computation error threshold.
	 */
	public static double DELTA = 1e-10;

	/**
	 * A fast and easy way to build a set.
	 * 
	 * @param <T>
	 *            The set elements type.
	 * @param values
	 *            The set elements.
	 * @return The newly constructed hash set.
	 */
	public static <T> Set<T> newSet(T... values) {
		return new HashSet<T>(Arrays.asList(values));
	}

	/**
	 * A fast and easy way to build a multi set.
	 * 
	 * @param <T>
	 *            The set elements type.
	 * @param values
	 *            The set of elements.
	 * @return The newly constructed hash multi set.
	 */
	public static <T> Multiset<T> newMultiset(T... values) {
		return HashMultiset.create(Arrays.asList(values));
	}

	/**
	 * A fast and easy way to build a list.It delegates to
	 * {@link Arrays#asList(Object...)}.
	 * 
	 * @param <T>
	 *            The set elements type.
	 * @param values
	 *            The set elements.
	 * @return The newly constructed hash set.
	 */
	public static <T> List<T> newList(T... values) {
		if (null == values) {
			return new ArrayList<T>();
		} else {
			return new ArrayList<T>(Arrays.asList(values));
		}
	}

	/**
	 * A fast and easy way to build a map out of two {@link List}s. It uses
	 * {@link LinkedHashMap} to preserve the order in which elements are added
	 * to the map, in case it is important for the tests.
	 * 
	 * @param <K>
	 *            The key type.
	 * @param <V>
	 *            The value type.
	 * @param keys
	 *            {@link List} of keys.
	 * @param values
	 *            {@link List} of values corresponding to the keys.
	 * @return Map with the given keys and values.
	 */
	public static <K, V> Map<K, V> newMap(List<K> keys, List<V> values) {
		Assert.assertEquals("Number of keys and values", keys.size(),
				values.size());

		Map<K, V> resultMap = new LinkedHashMap<K, V>(keys.size());
		for (int i = 0; i < keys.size(); i++) {
			resultMap.put(keys.get(i), values.get(i));
		}
		return resultMap;
	}

	/**
	 * A fast and easy way to build a map out of two list of key/value pairs.It
	 * uses {@link LinkedHashMap} to preserve the order in which elements are
	 * added to the map, in case it is important for the tests.
	 * 
	 * @param <T>
	 *            The set elements type.
	 * @param values
	 *            The set elements.
	 * @return The newly constructed hash set.
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> newMap(K key, V value, Object... keyValues) {
		Map<K, V> resultMap;
		if (null == keyValues || keyValues.length == 0) {
			resultMap = new LinkedHashMap<K, V>(1);
			resultMap.put(key, value);
		} else {
			if (keyValues.length % 2 != 0) {
				throw new IllegalArgumentException(
						"Number of arguments must be even");
			}
			resultMap = new LinkedHashMap<K, V>(keyValues.length / 2 + 1);
			resultMap.put(key, value);
			for (int i = 0; i < keyValues.length; i += 2) {
				resultMap.put((K) keyValues[i], (V) keyValues[i + 1]);
			}
		}
		return resultMap;
	}

	/**
	 * Converts any non-empty collection to native array of the collection's
	 * first element type.
	 * 
	 * @param <T>
	 *            The collection's element's type.
	 * @param source
	 *            The source non-empty collection.
	 * @return Native array of the type T (in fact of the first element's type).
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] toArray(Collection<T> source) {
		Utils.checkEmpty("Source collection", source);
		T first = source.iterator().next();
		return source.toArray((T[]) Array.newInstance(first.getClass(),
				source.size()));
	}
}
