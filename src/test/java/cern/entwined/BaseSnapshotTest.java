/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import static cern.entwined.test.TestUtils.newList;
import static cern.entwined.test.TestUtils.newMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import cern.entwined.exception.ConflictException;

/**
 * {@link BaseSnapshot} unit tests.
 * 
 * @author Ivan Koblik
 */
@SuppressWarnings( { "unchecked", "rawtypes" })
public class BaseSnapshotTest extends SnapshotAbstractTest {

    Map<Long, Object> testMap = Collections.unmodifiableMap(newMap(newList(1L, 2L), (List) newList(10L, 20L)));

    private BaseSnapshot<TestSnapshot> baseSnapshot;

    @Override
    protected Snapshot getSnapshot(long timestamp) {
        return new BaseSnapshot<TransactionalRef<Long>>(timestamp, new TransactionalRef<Long>(10l));
    }

    @Before
    public void beforeTest() {
        baseSnapshot = null;
    }

    @Test
    public void testBaseSnapshotV() {
        new BaseSnapshot<TestSnapshot>(0l, new TestSnapshot());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBaseSnapshotV_failNullArgument() {
        new BaseSnapshot<TestSnapshot>(0l, null);
    }

    @Test
    public void testBaseSnapshotVTransactionalMapOfLongObject() {
        testSnapshot = new TestSnapshot();
        transactionalMap = new TransactionalMap<Long, Object>();
        new BaseSnapshot<TestSnapshot>(0l, testSnapshot, transactionalMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBaseSnapshotVTransactionalMapOfLongObject_failNullSnapshot() {
        transactionalMap = new TransactionalMap<Long, Object>();
        new BaseSnapshot<TestSnapshot>(0l, null, transactionalMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBaseSnapshotVTransactionalMapOfLongObject_failNullGlobalMap() {
        testSnapshot = new TestSnapshot();
        new BaseSnapshot<TestSnapshot>(0l, testSnapshot, null);
    }

    @Test
    public void testGetGlobalMap_emptyDefaultMap() {
        testSnapshot = new TestSnapshot();
        baseSnapshot = new BaseSnapshot<TestSnapshot>(0l, testSnapshot);
        assertNotNull("Default empty map", baseSnapshot.getGlobalMap());
    }

    @Test
    public void testGetGlobalMap_argumentMap() {
        testSnapshot = new TestSnapshot();
        transactionalMap = new TransactionalMap<Long, Object>();
        baseSnapshot = new BaseSnapshot<TestSnapshot>(0l, testSnapshot, transactionalMap);
        assertSame("Passed transactional map", transactionalMap, baseSnapshot.getGlobalMap());
    }

    @Test
    public void testGetClientSnapshot_oneArgumentConstructor() {
        testSnapshot = new TestSnapshot();
        baseSnapshot = new BaseSnapshot<TestSnapshot>(0l, testSnapshot);
        assertSame("Registered the client snapshot", testSnapshot, baseSnapshot.getClientData());
    }

    @Test
    public void testGetClientSnapshot_twoArgumentConstructor() {
        testSnapshot = new TestSnapshot();
        baseSnapshot = new BaseSnapshot<TestSnapshot>(0l, testSnapshot);
        assertSame("Registered the client snapshot", testSnapshot, baseSnapshot.getClientData());
    }

    @Test
    public void testCleanCopy_delegatesForEveryField() {
        testSnapshot = new TestSnapshot(0, 0, Collections.EMPTY_MAP);
        transactionalMap = new TransactionalMap<Long, Object>(testMap);
        baseSnapshot = new BaseSnapshot<TestSnapshot>(0l, testSnapshot, transactionalMap);

        baseSnapshot.getClientData().getRef1().assoc(10);
        baseSnapshot.getGlobalMap().put(1L, 100L);

        BaseSnapshot<TestSnapshot> copy = baseSnapshot.cleanCopy();

        assertNotNull("The copy", copy);
        assertEquals("Copied client snapshot", (Integer) 0, copy.getClientData().getRef1().deref());
        assertEquals("Copied global map", testMap.get(1L), copy.getGlobalMap().get(1L));
    }

    @Test
    public void testCleanCopy_doesntShareSameCollections() {
        testSnapshot = new TestSnapshot(0, 0, Collections.EMPTY_MAP);
        transactionalMap = new TransactionalMap<Long, Object>(testMap);
        baseSnapshot = new BaseSnapshot<TestSnapshot>(0l, testSnapshot, transactionalMap);
        assertNotSame(baseSnapshot.getClientData(), baseSnapshot.cleanCopy().getClientData());
        assertNotSame(baseSnapshot.getGlobalMap(), baseSnapshot.cleanCopy().getGlobalMap());
    }

    @Test
    public void testDirtyCopy_delegatesForEveryField() {
        testSnapshot = new TestSnapshot(0, 0, Collections.EMPTY_MAP);
        transactionalMap = new TransactionalMap<Long, Object>(testMap);
        baseSnapshot = new BaseSnapshot<TestSnapshot>(0l, testSnapshot, transactionalMap);

        baseSnapshot.getClientData().getRef1().assoc(10);
        baseSnapshot.getGlobalMap().put(1L, 100L);

        BaseSnapshot<TestSnapshot> copy = baseSnapshot.dirtyCopy();

        assertNotNull("The copy", copy);
        assertEquals("Copied client snapshot", (Integer) 10, copy.getClientData().getRef1().deref());
        assertEquals("Copied global map", 100L, copy.getGlobalMap().get(1L));
    }

    @Test
    public void testDirtyCopy_doesntShareSameCollections() {
        testSnapshot = new TestSnapshot(0, 0, Collections.EMPTY_MAP);
        transactionalMap = new TransactionalMap<Long, Object>(testMap);
        baseSnapshot = new BaseSnapshot<TestSnapshot>(0l, testSnapshot, transactionalMap);
        assertNotSame(baseSnapshot.getClientData(), baseSnapshot.dirtyCopy().getClientData());
        assertNotSame(baseSnapshot.getGlobalMap(), baseSnapshot.dirtyCopy().getGlobalMap());
    }

    // ===================== Tests update() =====================

    private TestSnapshot testSnapshot;
    private TransactionalMap<Long, Object> transactionalMap;

    @Before
    public void cleanForUpdate() {
        testSnapshot = null;
        transactionalMap = null;
    }

    private void prepareSUT() {
        testSnapshot = new TestSnapshot(0, 0, Collections.EMPTY_MAP);
        transactionalMap = new TransactionalMap<Long, Object>(testMap);
        baseSnapshot = new BaseSnapshot<TestSnapshot>(0l, testSnapshot, transactionalMap);
    }

    private void prepareSUTForUpdateTest(boolean onlyLogs) {
        prepareSUT();

        baseSnapshot.getClientData().getRef1().assoc(10);
        baseSnapshot.getGlobalMap().put(1L, 100L);
        BaseSnapshot<TestSnapshot> copy = baseSnapshot.dirtyCopy();

        copy.getClientData().getRef1().assoc(11);
        copy.getGlobalMap().put(1L, 101L);

        baseSnapshot.update(copy, onlyLogs);
    }

    /**
     * Creates a global state that has 1 set to ref1 in client snapshot and no elements in global map.
     * 
     * @param clientSnapshot True if conflict need to be simulated in the client snapshot, false if in the global map.
     * @return The conflicting global state.
     */
    private BaseSnapshot<TestSnapshot> simulateConflict(boolean clientSnapshot) {
        TestSnapshot conflictingSnapshot = new TestSnapshot(1, 0, Collections.EMPTY_MAP);
        TransactionalMap<Long, Object> conflictingMap = new TransactionalMap<Long, Object>();
        if (clientSnapshot) {
            return new BaseSnapshot<TestSnapshot>(0l, conflictingSnapshot, transactionalMap.cleanCopy());
        } else {
            return new BaseSnapshot<TestSnapshot>(0l, testSnapshot.cleanCopy(), conflictingMap);
        }

    }

    @Test
    public void testUpdateBaseSnapshotOfVBooleanTrue_updatesOnlyLogs() {
        prepareSUTForUpdateTest(true);
        assertEquals("Old value preserved", (Integer) 10, baseSnapshot.getClientData().getRef1().deref());
        assertEquals("Old value preserved", 100L, baseSnapshot.getGlobalMap().get(1L));
    }

    @Test
    public void testUpdateBaseSnapshotOfVBooleanFalse_updatesValues() {
        prepareSUTForUpdateTest(false);
        assertEquals("New value", (Integer) 11, baseSnapshot.getClientData().getRef1().deref());
        assertEquals("New value", 101L, baseSnapshot.getGlobalMap().get(1L));
    }

    @Test(expected = ConflictException.class)
    public void testUpdateBaseSnapshotOfVBooleanFalse_delegatesForSnapshot() {
        prepareSUTForUpdateTest(false);
        baseSnapshot.commit(simulateConflict(true));
    }

    @Test(expected = ConflictException.class)
    public void testUpdateBaseSnapshotOfVBooleanFalse_delegatesForGlobalMap() {
        prepareSUTForUpdateTest(false);
        baseSnapshot.commit(simulateConflict(false));
    }

    @Test(expected = ConflictException.class)
    public void testUpdateBaseSnapshotOfVBooleanTrue_delegatesForSnapshot() {
        prepareSUTForUpdateTest(true);
        baseSnapshot.commit(simulateConflict(true));
    }

    @Test(expected = ConflictException.class)
    public void testUpdateBaseSnapshotOfVBooleanTrue_delegatesForGlobalMap() {
        prepareSUTForUpdateTest(true);
        baseSnapshot.commit(simulateConflict(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateBaseSnapshotOfVBooleanFalse_failNotForkedSnapshot() {
        prepareSUTForUpdateTest(true);

        testSnapshot = new TestSnapshot(0, 0, Collections.EMPTY_MAP);
        transactionalMap = new TransactionalMap<Long, Object>(testMap);

        BaseSnapshot<TestSnapshot> globalState = new BaseSnapshot<TestSnapshot>(0l, testSnapshot, transactionalMap);

        BaseSnapshot<TestSnapshot> committed = baseSnapshot.commit(globalState);
        prepareSUT();
        committed.update(baseSnapshot, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateBaseSnapshotOfVBooleanTrue_failNotForkedSnapshot() {
        prepareSUTForUpdateTest(true);

        testSnapshot = new TestSnapshot(0, 0, Collections.EMPTY_MAP);
        transactionalMap = new TransactionalMap<Long, Object>(testMap);

        BaseSnapshot<TestSnapshot> globalState = new BaseSnapshot<TestSnapshot>(0l, testSnapshot, transactionalMap);

        BaseSnapshot<TestSnapshot> committed = baseSnapshot.commit(globalState);
        prepareSUT();
        committed.update(baseSnapshot, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateBaseSnapshotOfVBoolean_notForkedSnapshot_usingDelatagesProtection() {
        prepareSUT();
        BaseSnapshot<TestSnapshot> baseSnapshot0 = baseSnapshot;

        prepareSUT();
        baseSnapshot.update(baseSnapshot0, true);
    }

    // ===================== Tests commit() =====================

    @Test
    public void testCommit_clientSnapshot() {
        prepareSUT();
        BaseSnapshot<TestSnapshot> committed = baseSnapshot.commit(simulateConflict(true));
        assertEquals("Client snapshot as in global state", (Integer) 1, committed.getClientData().getRef1().deref());

    }

    @Test
    public void testCommit_globalMap() {
        prepareSUT();
        BaseSnapshot<TestSnapshot> committed = baseSnapshot.commit(simulateConflict(false));
        assertEquals("Global map as in global state", (Integer) null, committed.getGlobalMap().get(1L));
    }

    @Test(expected = ConflictException.class)
    public void testCommit_failConflictInClientSnapshot() {
        prepareSUT();
        baseSnapshot.getClientData().getRef1().deref();
        baseSnapshot.commit(simulateConflict(true));

    }

    @Test(expected = ConflictException.class)
    public void testCommit_failConflictInGlobalMap() {
        prepareSUT();
        baseSnapshot.getGlobalMap().get(1L);
        baseSnapshot.commit(simulateConflict(false));
    }
}
