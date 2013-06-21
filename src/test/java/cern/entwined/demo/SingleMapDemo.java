/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */

package cern.entwined.demo;

import cern.entwined.Memory;
import cern.entwined.Transaction;
import cern.entwined.TransactionalMap;

/**
 * Demonstrates memory life cycle using single {@link TransactionalMap}.
 * 
 * @author Ivan Koblik
 */
public class SingleMapDemo {

    public static void main(String[] args) {
        Memory<TransactionalMap<Integer, Integer>> memory = new Memory<TransactionalMap<Integer, Integer>>(
                new TransactionalMap<Integer, Integer>());

        memory.runTransaction(new Transaction<TransactionalMap<Integer, Integer>>() {

            @Override
            public boolean run(TransactionalMap<Integer, Integer> data) throws Exception {
                System.out.println("Transaction starts");
                System.out.println("Value for key 1 " + data.get(1)); // -> null
                System.out.println("Value for key 2 " + data.get(2)); // -> null

                System.out.println("Adding pairs 1->10, 2->20");
                data.put(1, 10);
                data.put(2, 20);
                System.out.println("Value for key 1 " + data.get(1)); // -> 10
                System.out.println("Value for key 2 " + data.get(2)); // -> 20

                System.out.println("Removing key 2");
                data.remove(2);
                System.out.println("Value for key 1 " + data.get(1)); // -> 10
                System.out.println("Value for key 2 " + data.get(2)); // -> null

                // Returning true to:
                // 1. Check for conflicts.
                // 2. Commit changes.
                System.out.println("Transaction ends");
                return true;
            }

            @Override
            public void committed(TransactionalMap<Integer, Integer> data) throws Exception {
                System.out.println("Post-commit callback");
                System.out.println("Value for key 1 " + data.get(1)); // -> 10
                System.out.println("Value for key 2 " + data.get(2)); // -> null
            }
        });
    }
}
