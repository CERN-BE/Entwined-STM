/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import cern.entwined.exception.ConflictExceptionTest;
import cern.entwined.exception.InvocationExceptionTest;
import cern.entwined.exception.MemoryExceptionTest;
import cern.entwined.exception.NoTransactionExceptionTest;

/**
 * The route of all the STM unit tests.
 * 
 * @author Ivan Koblik
 */
@RunWith(Suite.class)
@SuiteClasses({ TransactionalMapTest.class, TransactionalRefTest.class,
		NodeTest.class, MemoryTest.class, SnapshotTest.class,
		CompositeCollectionTest.class, MemoryExceptionTest.class,
		ConflictExceptionTest.class, NoTransactionExceptionTest.class,
		InvocationExceptionTest.class, GlobalReferenceTest.class,
		BaseSnapshotTest.class, STMUtilsTest.class,
		TransactionAdapterTest.class, TransactionalQueueTest.class,
		TransactionalMultimapTest.class, TransactionClosureTest.class,
		UtilsTest.class })
public class AllSTMTests {

	/**
	 * Method for JUint 3 compatibility.
	 */
	public static Test suite() {
		return new JUnit4TestAdapter(AllSTMTests.class);
	}
}
