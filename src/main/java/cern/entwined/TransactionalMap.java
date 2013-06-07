/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import cern.entwined.exception.ConflictException;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 * Implementation of a transactional map. It logs all the reads and modifications of the data, and uses it at commit
 * time to detect conflicting transactions.
 * 
 * @param <K> The map key type.
 * @param <V> The map value type.
 * @author Ivan Koblik
 */
public class TransactionalMap<K, V> extends SemiPersistent<TransactionalMap<K, V>> implements OpaqueMap<K, V> {

    /**
     * The unmodifiable backbone of {@link TransactionalMap}.
     */
    private final Map<K, V> sourceMap;

    /**
     * Map of key value pairs of all locally added or modified values.
     */
    private final Map<K, V> pendingModifications = new HashMap<K, V>();

    /**
     * Set of keys of all the items locally removed from the map.
     */
    private final Set<K> pendingDeletions = new HashSet<K>();

    /**
     * Set of keys of all the items that were accessed or attempted to be accessed (i.e. for non-existent keys).
     */
    private final Set<K> accessed = new HashSet<K>();

    /**
     * This flag is used to mark the "entire world" as has been accessed. The reasoning is quite simple; if a user knows
     * all the entries in the map he also knows all entries that aren't there, which may not be true in the context of
     * the global map. Due to this {@link TransactionalMap} disallows commit if global state changes in any way even if
     * it is just a new entry.
     */
    private boolean globallyAccessed = false;

    /**
     * This flag is set to true the first time clear method is called.
     */
    private boolean cleared = false;

    /**
     * Constructs a new empty {@link TransactionalMap}.
     */
    @SuppressWarnings("unchecked")
    public TransactionalMap() {
        this(Collections.EMPTY_MAP, false);
    }

    /**
     * Constructs new {@link TransactionalMap} initializing it with the given collection. Passed collection is copied.
     * 
     * @param sourceMap The {@link TransactionalMap} initial state.
     */
    public TransactionalMap(Map<K, V> sourceMap) {
        this(sourceMap, true);
    }

    /**
     * Constructs new {@link TransactionalMap} initializing it with the given collection.
     * 
     * @param sourceMap The {@link TransactionalMap} initial state.
     * @param cloneSource If true passed collection is copied.
     */
    private TransactionalMap(Map<K, V> sourceMap, boolean cloneSource) {
        Utils.checkNull("Source map", sourceMap);
        if (cloneSource) {
            this.sourceMap = Collections.unmodifiableMap(new HashMap<K, V>(sourceMap));
        } else {
            this.sourceMap = sourceMap;
        }
    }

    @Override
    public int size() {
        this.markGloballyAccessed();
        Set<K> keys = Sets.union(this.sourceMap.keySet(), this.pendingModifications.keySet());
        return keys.size() - this.pendingDeletions.size();
    }

    @Override
    public boolean isEmpty() {
        boolean empty = (this.sourceMap.size() == this.pendingDeletions.size()) && this.pendingModifications.isEmpty();
        if (empty) {
            this.markGloballyAccessed();
        }
        return empty;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.OpaqueMap#clear()
     */
    @Override
    public void clear() {
        this.markCleared();
        this.pendingDeletions.addAll(sourceMap.keySet());
        this.pendingModifications.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.OpaqueMap#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(K key) {
        this.markAccessed(key);
        return (sourceMap.containsKey(key) || this.pendingModifications.containsKey(key))
                && !pendingDeletions.contains(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.OpaqueMap#get(java.lang.Object)
     */
    @Override
    public V get(K key) {
        this.markAccessed(key);
        if (this.pendingDeletions.contains(key)) {
            return null;
        } else if (this.pendingModifications.containsKey(key)) {
            return this.pendingModifications.get(key);
        }
        return this.sourceMap.get(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.OpaqueMap#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public V put(K key, V value) {
        this.accessed.add(key);
        this.pendingDeletions.remove(key);
        V oldValue = this.pendingModifications.put(key, value);
        return null != oldValue ? oldValue : this.sourceMap.get(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.OpaqueMap#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        Utils.checkNull("Map", m);
        this.accessed.addAll(m.keySet());
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.OpaqueMap#remove(java.lang.Object)
     */
    @Override
    public V remove(K key) {
        V oldValue = this.get(key); // Getting old value and marking it as accessed
        if (this.sourceMap.containsKey(key)) {
            this.pendingDeletions.add(key);
        }
        this.pendingModifications.remove(key);
        return oldValue;
    }

    //
    // Views
    //

    @Override
    public Set<K> keySet() {
        return new KeySet();
    }

    //
    // Transactional methods
    //

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.Transactional#cleanCopy()
     */
    @Override
    public TransactionalMap<K, V> cleanCopy() {
        return new TransactionalMap<K, V>(this.sourceMap, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.SemiPersistent#dirtyCopy()
     */
    @Override
    protected TransactionalMap<K, V> dirtyCopy() {
        TransactionalMap<K, V> copy = new TransactionalMap<K, V>(this.sourceMap, false);
        copy.globallyAccessed = this.globallyAccessed;
        copy.markAccessed(this.accessed);
        copy.pendingDeletions.addAll(this.pendingDeletions);
        copy.pendingModifications.putAll(this.pendingModifications);
        return copy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.SemiPersistent#update(java.lang.Object, boolean)
     */
    @Override
    protected void update(TransactionalMap<K, V> changes, boolean onlyReadLogs) {
        Utils.checkNull("Local changes", changes);
        if (this.sourceMap != changes.sourceMap) {
            throw new IllegalArgumentException("Updates are only possible for collections with the same source");
        }
        if (changes.globallyAccessed) {
            markGloballyAccessed();
        }
        this.markAccessed(changes.accessed);
        if (!onlyReadLogs) {
            this.pendingModifications.clear();
            this.pendingModifications.putAll(changes.pendingModifications);
            this.pendingDeletions.clear();
            this.pendingDeletions.addAll(changes.pendingDeletions);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.ConflictAware#commit(java.lang.Object)
     */
    @Override
    public TransactionalMap<K, V> commit(TransactionalMap<K, V> globalState) {
        Utils.checkNull("Global state", globalState);
        if (!globalState.pendingDeletions.isEmpty() || !globalState.pendingModifications.isEmpty()
                || !globalState.accessed.isEmpty() || globalState.globallyAccessed) {
            throw new IllegalArgumentException("Global state map must be commited before calling this method");
        }

        // Checking for conflicts
        if (this.globallyAccessed) {
            if (!globalState.sourceMap.equals(this.sourceMap)) {
                throw new ConflictException("All the items of this map have been accessed "
                        + "this prohibits commit in the case of concurrent changes");
            }
        }
        for (K key : this.accessed) {
            checkConsistency(globalState.sourceMap, key);
        }

        // Return current global state if there are no local modifications
        if (this.pendingDeletions.isEmpty() && this.pendingModifications.isEmpty()) {
            return globalState;
        }

        // Getting a copy of the global map
        HashMap<K, V> globalMapCopy = new HashMap<K, V>(globalState.sourceMap);

        // Apply addition or modification
        for (Entry<K, V> entry : this.pendingModifications.entrySet()) {
            globalMapCopy.put(entry.getKey(), entry.getValue());
        }

        // Apply deletion
        for (K key : this.pendingDeletions) {
            globalMapCopy.remove(key);
        }

        // Returning a new instance of the map
        return new TransactionalMap<K, V>(globalMapCopy);
    }

    //
    // Private methods
    //

    /**
     * Marks given key as accessed.
     * 
     * @param key The key to mark as accessed.
     */
    private void markAccessed(K key) {
        if (!this.globallyAccessed) {
            this.accessed.add(key);
        }
    }

    /**
     * Marks given collection of keys as accessed.
     * 
     * @param key The keys to mark as accessed.
     */
    private void markAccessed(Collection<K> keys) {
        if (!this.globallyAccessed) {
            this.accessed.addAll(keys);
        }
    }

    /**
     * Marks the entire space of keys as accessed unless the map has been cleared.
     */
    private void markGloballyAccessed() {
        if (!this.cleared) {
            // Global access is allowed after the map has been cleared.
            this.globallyAccessed = true;
            this.accessed.clear();
        }
    }

    /**
     * Marks the map as cleared and marks all its items as accessed unless it has already been globally accessed.
     */
    private void markCleared() {
        if (!this.globallyAccessed) {
            this.cleared = true;
            this.markAccessed(sourceMap.keySet());
        }
    }

    /**
     * Simply checks if values corresponding to the key are the <b>same</b> in the global and source maps.
     * 
     * @param globalMap The global map.
     * @param key The key corresponding to the value to be checked.
     */
    private void checkConsistency(Map<K, V> globalMap, K key) {
        V sourceValue = this.sourceMap.get(key);
        V globalValue = globalMap.get(key);
        if ((sourceValue != globalValue) || // <br>
                ((null == sourceValue || null == globalValue)// <br>
                && (this.sourceMap.containsKey(key) ^ globalMap.containsKey(key)))) {
            throw new ConflictException("Conflicting changes for [" + key + "]");
        }
    }

    /**
     * Dynamic view on the keys of the map.
     * 
     * @author Ivan Koblik
     */
    private class KeySet extends AbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return TransactionalMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return TransactionalMap.this.isEmpty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean contains(Object o) {
            return containsKey((K) o);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(Object o) {
            boolean result = TransactionalMap.this.containsKey((K) o);
            TransactionalMap.this.remove((K) o);
            return result;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean modified = false;
            for (Iterator<?> i = c.iterator(); i.hasNext();) {
                modified |= this.remove(i.next());
            }
            return modified;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            TransactionalMap.this.clear();
        }
    }

    /**
     * Iterator over the keys of the map.
     * 
     * @author Ivan Koblik
     */
    private class KeyIterator implements Iterator<K> {
        /**
         * The delegate iterator that is a concatenation of sourceMap and pendingModifications minus pendingDeletions.
         */
        private final Iterator<K> keyIterator;

        /**
         * Constructs the iterator by initializing the delegate iterator.
         */
        public KeyIterator() {
            // Concatenate iterators of sourceMap and pendingModifications.
            Iterator<K> unfiltered = Iterators.concat(TransactionalMap.this.sourceMap.keySet().iterator(),
                    TransactionalMap.this.pendingModifications.keySet().iterator());
            // Remove the elements from pendingDeletions.
            keyIterator = Iterators.filter(unfiltered, not(in(TransactionalMap.this.pendingDeletions)));
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = keyIterator.hasNext();
            if (!hasNext) {
                TransactionalMap.this.markGloballyAccessed();
            }
            return hasNext;
        }

        @Override
        public K next() {
            try {
                return keyIterator.next();
            } catch (NoSuchElementException ex) {
                TransactionalMap.this.markGloballyAccessed();
                throw ex;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
