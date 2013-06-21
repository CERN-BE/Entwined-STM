/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;


import com.google.common.base.Function;

/**
 * This class is aimed at simplification of client collection and snapshot implementations. Simple adapter mapping array
 * index to transactional collection would suffice.
 * <p>
 * <b>DISCLAIMER</b> This collection must never be used by clients directly! Use it only to implement custom collections
 * and snapshots.
 * 
 * @author Ivan Koblik
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class CompositeCollection extends SemiPersistent<CompositeCollection> {

    /**
     * Array of transactional references or collections.
     */
    private final SemiPersistent<SemiPersistent>[] references;

    /**
     * Array of references that have been accessed by the client.
     * <p>
     * Access flags may only be modified in the constructor's body or in {@link CompositeCollection#unsafeGet(int)}
     * other methods are not allowed to modify them.
     */
    private final boolean[] accessed;

    /**
     * Function must be used to perform first time access to a reference.
     */
    private final Function<Integer, SemiPersistent<SemiPersistent>> initialAccessor;

    /**
     * Creates a new {@link CompositeCollection} with given references.
     * 
     * @param references Transactional references or collections. Elements cannot be null.
     */
    public CompositeCollection(SemiPersistent... references) {
        Utils.checkNull("References", references);
        if (0 == references.length) {
            throw new IllegalArgumentException("At least one reference must be specified");
        }

        // Verify that all items are non null.
        for (int i = 0; i < references.length; i++) {
            SemiPersistent value = references[i];
            if (null == value) {
                throw new IllegalArgumentException("Value at " + i + " is null");
            }
        }

        this.references = this.copyReferences(references);

        // By default all values in the array are false, see java spec 4.5.5 "Initial Values of Variables"
        this.accessed = new boolean[references.length];
        this.initialAccessor = new SimpleGet();
    }

    /**
     * Creates a new {@link CompositeCollection} from either a clean or dirty copy of the original collection.
     * 
     * @param originalCollection The original collection.
     * @param cleanCopy True if clean copy, false if dirty copy.
     */
    private CompositeCollection(CompositeCollection originalCollection, boolean cleanCopy) {
        this.references = this.copyReferences(originalCollection.references);
        if (cleanCopy) {
            this.initialAccessor = new CleanCopy();
            this.accessed = new boolean[originalCollection.accessed.length];
        } else {
            this.initialAccessor = new DirtyCopy();
            this.accessed = this.copyAccesses(originalCollection.accessed);
            for (int i = 0; i < this.references.length; i++) {
                if (this.accessed[i]) {
                    this.references[i] = this.references[i].dirtyCopy();
                }
            }
        }
    }

    /**
     * Creates a new {@link CompositeCollection} committing localCollection into globalState.
     * 
     * @param localCollection The local collection.
     * @param globalState The global state.
     */
    private CompositeCollection(CompositeCollection localCollection, CompositeCollection globalState) {
        // Copy global state's references, and update accessed references later.
        this.initialAccessor = new SimpleGet();
        this.references = this.copyReferences(globalState.references);
        this.accessed = new boolean[this.references.length];

        for (int i = 0; i < this.references.length; i++) {
            if (localCollection.accessed[i]) {
                // Commit only references that have been accessed.
                references[i] = localCollection.references[i].commit(globalState.unsafeGet(i));
            }
        }
    }

    /**
     * {@inheritDoc} <br>
     * <i>Visibility is changed to public to let client collections use STM library collections through this composite,
     * but it also means that client collection must not expose this class.</i>
     */
    @Override
    public final CompositeCollection cleanCopy() {
        return new CompositeCollection(this, true);
    }

    /**
     * {@inheritDoc} <br>
     * <i>Visibility is changed to public to let client collections use STM library collections through this composite,
     * but it also means that client collection must not expose this class.</i>
     */
    @Override
    public final CompositeCollection dirtyCopy() {
        return new CompositeCollection(this, false);
    }

    /**
     * {@inheritDoc} <br>
     * <i>Visibility is changed to public to let client collections use STM library collections through this composite,
     * but it also means that client collection must not expose this class.</i>
     */
    public void update(CompositeCollection changes, boolean onlyReadLogs) {
        Utils.checkNull("Updated collections", changes);

        for (int i = 0; i < this.references.length; i++) {
            if (changes.accessed[i]) {
                this.unsafeGet(i).update(changes.references[i], onlyReadLogs);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see cern.oasis.server.stm.Transactional#commit(java.lang.Object)
     */
    @Override
    public final CompositeCollection commit(CompositeCollection globalState) {
        if (!this.isAccessed()) {
            // No references have been accessed, return the global state as it is.
            return globalState;
        } else {
            return new CompositeCollection(this, globalState);
        }
    }

    /**
     * Returns a transactional reference or collection with the give index.
     * <p>
     * <b>Note:</b> Be very careful with this method! Due to the way generics are implemented in Java, no real type
     * checking is performed. This method silently casts element to the desired type.
     * <p>
     * See the example below, both lines of code are getting element number zero from the same collection, none of them
     * will be statically checked by the compiler , but <b>it is certain that in runtime at least one of them is due to
     * fail</b>.
     * 
     * <pre>
     * TransactionalRef&lt;Integer&gt; ref = compositeCollection.get(0);
     * TransactionalMap&lt;Integer, Double&gt; map = compositeCollection.get(0);
     * </pre>
     * 
     * @param <T> The desired type of the resulting value.
     * @param idx The index.
     * @return Requested transactional reference or collection.
     */
    public final <T extends SemiPersistent> T get(int idx) {
        if (idx < 0 || idx >= this.references.length) {
            throw new IllegalArgumentException("Index " + idx + " is out of bounds");
        }
        return this.<T> unsafeGet(idx);
    }

    /**
     * This method should be used internally instead of {@link CompositeCollection#get(int)}.
     * 
     * @param <T> The desired type of the resulting value.
     * @param idx The index.
     * @return Requested transactional reference or collection.
     */
    private <T extends SemiPersistent> T unsafeGet(int idx) {
        if (this.accessed[idx]) {
            return (T) this.references[idx];
        } else {
            T value = (T) this.initialAccessor.apply(idx);
            this.accessed[idx] = true;
            this.references[idx] = value;
            return value;
        }
    }

    /**
     * Copies array of transactional references and collections.
     * 
     * @param source The source array.
     * @return Copy of the source array.
     */
    private final SemiPersistent<SemiPersistent>[] copyReferences(SemiPersistent<SemiPersistent>[] source) {
        SemiPersistent<SemiPersistent>[] copy = new SemiPersistent[source.length];
        System.arraycopy(source, 0, copy, 0, source.length);
        return copy;
    }

    /**
     * Copies the references access log.
     * 
     * @param access The source array.
     * @return Copy of the source array.
     */
    private final boolean[] copyAccesses(boolean[] access) {
        boolean[] copy = new boolean[access.length];
        System.arraycopy(access, 0, copy, 0, access.length);
        return copy;
    }

    /**
     * Returns true if any of the collection's items have have been accessed.
     * 
     * @return <code>true</code> if at least one of the collection's items has been accessed.
     */
    private final boolean isAccessed() {
        for (int i = 0; i < this.accessed.length; i++) {
            if (this.accessed[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reference access strategy that simply returns the reference.
     * 
     * @author Ivan Koblik
     */
    private class SimpleGet implements Function<Integer, SemiPersistent<SemiPersistent>> {
        @Override
        public SemiPersistent<SemiPersistent> apply(Integer idx) {
            return CompositeCollection.this.references[idx];
        }
    }

    /**
     * Reference access strategy that returns a cleanCopy of the reference.
     * 
     * @author Ivan Koblik
     */
    private class CleanCopy implements Function<Integer, SemiPersistent<SemiPersistent>> {
        @Override
        public SemiPersistent<SemiPersistent> apply(Integer idx) {
            return CompositeCollection.this.references[idx].cleanCopy();
        }
    }

    /**
     * Reference access strategy that returns a dirtyCopy the reference.
     * 
     * @author Ivan Koblik
     */
    private class DirtyCopy implements Function<Integer, SemiPersistent<SemiPersistent>> {
        @Override
        public SemiPersistent<SemiPersistent> apply(Integer idx) {
            return CompositeCollection.this.references[idx].dirtyCopy();
        }
    }
}
