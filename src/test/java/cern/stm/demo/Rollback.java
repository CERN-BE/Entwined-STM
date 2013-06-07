/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm.demo;

import java.util.Collections;

import org.junit.Test;

import cern.stm.Memory;
import cern.stm.TestSnapshot;
import cern.stm.TransactionAdapter;

public class Rollback {

    @SuppressWarnings("unchecked")
    Memory<TestSnapshot> memory = new Memory<TestSnapshot>(new TestSnapshot(1, 1, Collections.EMPTY_MAP));

    @Test
    public void testRollback() {
        for (int i = 0; i < 100; i++) {
            try {
                memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
                    @Override
                    public boolean run(TestSnapshot snapshot) {
                        snapshot.getRef1().assoc(100);
                        snapshot.getRef2().assoc(101);
                        snapshot.getMap().put(1, 111);
                        throw new RuntimeException();
                    }
                });
            } catch (RuntimeException ex) {
                // Ignoring the exception
            }
        }

        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public void committed(TestSnapshot snapshot) {
                System.out.println(snapshot.getRef1().deref());
                System.out.println(snapshot.getRef2().deref());
                System.out.println(snapshot.getMap().get(1));
            }
        });
    }
}
