/*
 * Entwined STM
 * 
 * (c) Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined.demo;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import cern.entwined.Memory;
import cern.entwined.TestSnapshot;
import cern.entwined.Transaction;
import cern.entwined.TransactionAdapter;
import cern.entwined.TransactionalRef;

public class Fibonacci {

    volatile int a1 = 1, a2 = 1;

    @SuppressWarnings("unchecked")
    Memory<TestSnapshot> memory = new Memory<TestSnapshot>(new TestSnapshot(1, 1, Collections.EMPTY_MAP));

    Transaction<TestSnapshot> transaction = new TransactionAdapter<TestSnapshot>() {
        @Override
        public boolean run(TestSnapshot snapshot) {
            TransactionalRef<Integer> v1 = snapshot.getRef1();
            TransactionalRef<Integer> v2 = snapshot.getRef2();
            if (v1.deref() >= v2.deref()) {
                snapshot.getRef2().assoc(v1.deref() + v2.deref());
            } else {
                int t = (v1.deref() + v2.deref());
                snapshot.getRef1().assoc(snapshot.getRef2().deref());
                sleep(10);
                snapshot.getRef2().assoc(t);
            }
            return true;
        }

        @Override
        public void committed(TestSnapshot snapshot) {
            System.out.println(String.format("v1 =% 6d, v2 =% 6d", snapshot.getRef1().deref(), snapshot.getRef2()
                    .deref()));
        }
    };

    Runnable transactionalUpdates = new Runnable() {
        public void run() {
            memory.runTransaction(transaction);
        };
    };

    Runnable asynchronousUpdates = new Runnable() {
        public void run() {
            if (a1 >= a2) {
                a2 = (a1 + a2);
            } else {
                int t = (a1 + a2);
                a1 = a2;
                sleep(10);
                a2 = t;
            }
            System.out.println(String.format("a1 =% 6d, a2 =% 6d", a1, a2));
        };
    };

    private void runInThreadPool(Runnable runnable) {
        final int numThreads = 10;
        final int numTimes = 20;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10000);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.SECONDS, queue);

        for (int j = 0; j < numTimes; j++) {
            executor.execute(runnable);
        }
        while (true) {
            if (executor.getCompletedTaskCount() == numTimes) {
                break;
            }
            sleep(1);
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    @Test
    public void transactionalTest() throws Exception {
        System.out.println("Transactional Fibonacci");
        runInThreadPool(transactionalUpdates);
    }

    @Test
    public void asynchronousTest() throws Exception {
        System.out.println("Asynchronized Fibonacci");
        runInThreadPool(asynchronousUpdates);
    }
}
