/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * The opaque (impossible to query for all the items or get the size of the collection) transactional multimap
 * implementation.
 * 
 * @author Ivan Koblik
 */
public class TransactionalMultimap<K, V> extends SemiPersistent<TransactionalMultimap<K, V>> implements
        OpaqueMultimap<K, V> {

    /**
     * The {@link TransactionalMap} instance actually storing the data.
     */
    private final TransactionalMap<K, Set<V>> delegate;

    /**
     * Constructs a new empty {@link TransactionalMultimap}.
     */
    public TransactionalMultimap() {
        this(new TransactionalMap<K, Set<V>>());
    }

    private TransactionalMultimap(TransactionalMap<K, Set<V>> source) {
        this.delegate = source;
    }

    //
    // Overridden methods
    //

    /**
     * Delegates to {@link TransactionalMap#size()}.
     */
    @Override
    public int size() {
        return this.delegate.size();
    }

    /**
     * Delegates to {@link TransactionalMap#isEmpty()}.
     */
    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    /**
     * Delegates to {@link TransactionalMap#get(Object)} replacing the returned <code>null</code> value with an empty
     * set.
     */
    @Override
    public Set<V> get(K key) {
        Set<V> result = this.delegate.get(key);
        if (null != result) {
            return result;
        } else {
            return ImmutableSet.<V> of();
        }
    };

    /**
     * Stores given pair in the map, extending set associated with the key, if it already existed. This method doesn't
     * perform null checks and doesn't copy the given collection.
     * 
     * @param key The key under which to store the values.
     * @param value The values for the key.
     * @return The old set of values replaced by the method.
     */
    private Set<V> unsafePut(K key, Set<V> value) {
        if (value.isEmpty()) {
            return this.remove(key);
        } else {
            Set<V> replaced = this.delegate.put(key, Collections.unmodifiableSet(value));
            return null != replaced ? replaced : ImmutableSet.<V> of();
        }
    };

    /**
     * Delegates to {@link TransactionalMap#remove(Object)} replacing the returned <code>null</code> value with an empty
     * set.
     */
    @Override
    public Set<V> remove(K key) {
        Set<V> result = this.delegate.remove(key);
        return null != result ? result : ImmutableSet.<V> of();
    };

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public boolean containsKey(K key) {
        return this.delegate.containsKey(key);
    };

    /**
     * Overrides the default behavior of the method by merging the immutable sets for the same key.
     */
    @Override
    public void putAll(Map<? extends K, ? extends Set<V>> map) {
        Utils.checkNull("Inserted map", map);
        for (Entry<? extends K, ? extends Set<V>> pair : map.entrySet()) {
            this.putAll(pair.getKey(), pair.getValue());
        }
    }

    //
    // Views
    //

    /**
     * Delegates to {@link TransactionalMap#keySet()}.
     */
    @Override
    public Set<K> keySet() {
        return this.delegate.keySet();
    }

    //
    // Additional methods
    //

    /**
     * Adds the value to the entry with the given key.
     * 
     * @param key The key that the set is stored under.
     * @param value The value that is added to the entry with the given key.
     * @return The replaced set or an empty set if there was no entry with the given key.
     */
    @Override
    public Set<V> put(K key, V value) {
        Set<V> oldSet = this.delegate.get(key);
        if (null != oldSet) {
            HashSet<V> newSet = new HashSet<V>(oldSet);
            newSet.add(value);
            return this.unsafePut(key, newSet);
        } else {
            return this.unsafePut(key, Collections.singleton(value));
        }
    };

    /**
     * Puts all the values in the collection with the given key.
     * 
     * @param key The key that the set is stored under.
     * @param values The values that are added to the entry with the given key.
     * @return The replaced set or an empty set if there was no entry with the given key.
     */
    public Set<V> putAll(K key, Collection<V> values) {
        Utils.checkNull("Values", values);
        Set<V> oldSet = this.delegate.get(key);
        HashSet<V> newSet;
        if (null != oldSet) {
            newSet = new HashSet<V>(oldSet);
            newSet.addAll(values);
        } else {
            newSet = new HashSet<V>(values);
        }
        return this.unsafePut(key, newSet);
    }

    /**
     * Removes the given value from the entry with the given key. If there are no more values for the key, removes the
     * whole entry from the map.
     * 
     * @param key The key to retrieve the set.
     * @param value Value to be removed from the set.
     * @return The replaced set or an empty set if there was no entry with the given key.
     */
    public Set<V> remove(K key, V value) {
        Set<V> oldSet = this.delegate.get(key);
        if (null != oldSet) {
            Set<V> newSet = Sets.difference(oldSet, Collections.singleton(value)).immutableCopy();
            return this.unsafePut(key, newSet);
        }
        return this.remove(key);
    };

    //
    // Transactional methods
    //

    @Override
    public TransactionalMultimap<K, V> commit(TransactionalMultimap<K, V> globalState) {
        return new TransactionalMultimap<K, V>(this.delegate.commit(globalState.delegate));
    }

    @Override
    protected TransactionalMultimap<K, V> cleanCopy() {
        return new TransactionalMultimap<K, V>(this.delegate.cleanCopy());
    }

    @Override
    protected TransactionalMultimap<K, V> dirtyCopy() {
        return new TransactionalMultimap<K, V>(this.delegate.dirtyCopy());
    }

    @Override
    protected void update(TransactionalMultimap<K, V> changes, boolean onlyReadLogs) {
        this.delegate.update(changes.delegate, onlyReadLogs);
    }
}
