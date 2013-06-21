/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined.demo;

import cern.entwined.Memory;
import cern.entwined.TransactionAdapter;
import cern.entwined.TransactionalRef;

/**
 * Shows that transaction will be rolled back if it terminates with an exception.
 * 
 * @author Ivan Koblik
 */
public class RollbackDemo {

    public static void main(String[] args) {
        Memory<TransactionalRef<Integer>> memory = new Memory<TransactionalRef<Integer>>(
                new TransactionalRef<Integer>());

        try {
            memory.runTransaction(new TransactionAdapter<TransactionalRef<Integer>>() {
                @Override
                public boolean run(TransactionalRef<Integer> reference) {
                    System.out.println("Transaction starts");
                    System.out.println("Setting reference to 100");
                    reference.assoc(100);
                    throw new RuntimeException();
                }
            });
        } catch (RuntimeException ex) {
            System.out.println("Transaction failed with RuntimeException");
        }

        memory.runTransaction(new TransactionAdapter<TransactionalRef<Integer>>() {
            @Override
            public void committed(TransactionalRef<Integer> reference) {
                System.out.println("Reading reference value: " + reference.deref()); // -> null
            }
        });
    }
}
