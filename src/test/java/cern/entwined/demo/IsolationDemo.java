/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */

package cern.entwined.demo;

import cern.entwined.Memory;
import cern.entwined.TransactionAdapter;
import cern.entwined.TransactionalMap;

/**
 * Demonstrates isolation property of the memory.
 * 
 * @author Ivan Koblik
 */
public class IsolationDemo {

    public static void main(String[] args) {
        final Memory<TransactionalMap<Integer, Integer>> memory = new Memory<TransactionalMap<Integer, Integer>>(
                new TransactionalMap<Integer, Integer>());

        // This thread will run in the middle of another transaction.
        final Thread concurrentRead = new Thread() {
            @Override
            public void run() {
                memory.runTransaction(new TransactionAdapter<TransactionalMap<Integer, Integer>>() {
                    @Override
                    public boolean run(TransactionalMap<Integer, Integer> data) throws Exception {
                        System.out.println("Transaction 2 starts");
                        System.out.println("Transaction 2: Value for key 1 " + data.get(1)); // -> null
                        System.out.println("Transaction 2 ends");
                        return true;
                    }
                });
            }
        };

        // Modifies current state of the map and runs a concurrent transaction.
        memory.runTransaction(new TransactionAdapter<TransactionalMap<Integer, Integer>>() {

            @Override
            public boolean run(TransactionalMap<Integer, Integer> data) throws Exception {
                System.out.println("Transaction 1 starts");
                System.out.println("Adding pair 1->10");
                data.put(1, 10);
                System.out.println("Transaction 1: Value for key 1 " + data.get(1)); // -> 10

                concurrentRead.start();
                concurrentRead.join();

                System.out.println("Transaction 2 ends");
                // Returning true to:
                // 1. Check for conflicts.
                // 2. Commit.
                return true;
            }
        });
    }
}
