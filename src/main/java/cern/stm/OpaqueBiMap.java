/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm;

import java.util.Map;

/**
 * A map like interface to facilitate implementation of {@link TransactionalMap}.
 * 
 * @param <K> The map key type.
 * @param <V> The map value type.
 * @author Ivan Koblik
 */
public interface OpaqueBiMap<K, V> {

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key. More formally, returns <tt>true</tt>
     * if and only if this map contains a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>. (There can be at most one such mapping.)
     * 
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified key
     * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
     */
    boolean containsKey(K key);

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the
     * key.
     * <p>
     * More formally, if this map contains a mapping from a key {@code k} to a value {@code v} such that {@code
     * (key==null ? k==null : key.equals(k))}, then this method returns {@code v}; otherwise it returns {@code null}.
     * (There can be at most one such mapping.)
     * <p>
     * If this map permits null values, then a return value of {@code null} does not <i>necessarily</i> indicate that
     * the map contains no mapping for the key; it's also possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to distinguish these two cases.
     * 
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the
     *         key
     * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
     */
    V get(K key);

    /**
     * Associates the specified value with the specified key in this map (optional operation). If the map previously
     * contained a mapping for the key, the old value is replaced by the specified value. (A map <tt>m</tt> is said to
     * contain a mapping for a key <tt>k</tt> if and only if {@link #containsKey(Object) m.containsKey(k)} would return
     * <tt>true</tt>.)
     * 
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for
     *         <tt>key</tt>. (A <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt>
     *         with <tt>key</tt>, if the implementation supports <tt>null</tt> values.)
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by this map
     * @throws ClassCastException if the class of the specified key or value prevents it from being stored in this map
     * @throws NullPointerException if the specified key or value is null and this map does not permit null keys or
     *             values
     * @throws IllegalArgumentException if some property of the specified key or value prevents it from being stored in
     *             this map
     */
    V put(K key, V value);

    /**
     * Removes the mapping for a key from this map if it is present (optional operation). More formally, if this map
     * contains a mapping from key <tt>k</tt> to value <tt>v</tt> such that
     * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping is removed. (The map can contain at most one
     * such mapping.)
     * <p>
     * Returns the value to which this map previously associated the key, or <tt>null</tt> if the map contained no
     * mapping for the key.
     * <p>
     * If this map permits null values, then a return value of <tt>null</tt> does not <i>necessarily</i> indicate that
     * the map contained no mapping for the key; it's also possible that the map explicitly mapped the key to
     * <tt>null</tt>.
     * <p>
     * The map will not contain a mapping for the specified key once the call returns.
     * 
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for
     *         <tt>key</tt>.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is not supported by this map
     * @throws NullPointerException if the specified key is null and this map does not permit null keys (optional)
     */
    V remove(K key);

    /**
     * Copies all of the mappings from the specified map to this map (optional operation). The effect of this call is
     * equivalent to that of calling {@link #put(Object,Object) put(k, v)} on this map once for each mapping from key
     * <tt>k</tt> to value <tt>v</tt> in the specified map. The behavior of this operation is undefined if the specified
     * map is modified while the operation is in progress.
     * 
     * @param m mappings to be stored in this map
     * @throws UnsupportedOperationException if the <tt>putAll</tt> operation is not supported by this map
     * @throws ClassCastException if the class of a key or value in the specified map prevents it from being stored in
     *             this map
     * @throws IllegalArgumentException if the specified map is null, or if this map does not permit null keys or
     *             values, and the specified map contains null keys or values
     */
    void putAll(Map<? extends K, ? extends V> m);

    /**
     * Removes all of the mappings from this map (optional operation). The map will be empty after this call returns.
     * 
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation is not supported by this map
     */
    void clear();

    /**
     * Returns the inverse view of this bimap, which maps each of this bimap's values to its associated key. The two
     * bimaps are backed by the same data; any changes to one will appear in the other.
     * <p>
     * <b>Note:</b>There is no guaranteed correspondence between the iteration order of a bimap and that of its inverse.
     * 
     * @return the inverse view of this bimap
     */
    OpaqueBiMap<K, V> inverse();
}
