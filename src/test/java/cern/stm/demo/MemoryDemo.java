/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.stm.demo;

import static cern.stm.demo.Fibonacci.sleep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import cern.stm.Memory;
import cern.stm.TestSnapshot;
import cern.stm.Transaction;
import cern.stm.TransactionAdapter;
import cern.stm.TransactionalMap;

/**
 * Memory demo.
 * 
 * @author Ivan Koblik
 */
public class MemoryDemo {

    static TestSnapshot oasisSnapshot = new TestSnapshot();
    static final Memory<TestSnapshot> memory = new Memory<TestSnapshot>(oasisSnapshot);

    private static AtomicInteger staticCounter = new AtomicInteger(0);
    private static int NN = 20;
    final int NUMTIMES = 10;

    static class CustomTransaction implements Transaction<TestSnapshot> {
        private int n = staticCounter.getAndIncrement();

        @Override
        public void committed(TestSnapshot snapshot) {
            String res = "";
            for (int i = 0; i < NN; i++) {
                res += snapshot.getMap().get(i) + ",";
            }
            System.out.println(res);
            System.out.println("-----");
        }

        @Override
        public boolean run(TestSnapshot snapshot) {
            TransactionalMap<Integer, Integer> map = snapshot.getMap();
            map.put(n, map.get(n) + 1);
            sleep(100);
            return true;
        }
    };

    @Test(timeout = 60000)
    public void testRunTransaction_multiThreaded() throws Exception {
        final int numThreads = 10;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10000);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.SECONDS, queue);
        final List<CustomTransaction> transactions = new ArrayList<CustomTransaction>();
        for (int i = 0; i < NN; i++) {
            CustomTransaction transaction = new CustomTransaction();
            transactions.add(transaction);
        }

        // Initialize shared memory
        memory.runTransaction(new TransactionAdapter<TestSnapshot>() {
            @Override
            public boolean run(TestSnapshot snapshot) {
                for (int i = 0; i < NN; i++) {
                    snapshot.getMap().put(i, 0);
                }
                return true;
            }
        });

        long time = System.currentTimeMillis();
        for (int j = 0; j < NUMTIMES; j++) {
            for (int i = 0; i < NN; i++) {
                final int k = i;
                // memory.runTransaction(transactions.get(k));
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        memory.runTransaction(transactions.get(k));
                    }
                });
            }
        }
        while (true) {
            if (executor.getCompletedTaskCount() == NUMTIMES * NN) {
                break;
            }
            sleep(1);
        }
        System.out.println("Transactional millis: " + (System.currentTimeMillis() - time));
    }

    @Test
    public void testSync() throws Exception {
        final int numThreads = 10;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10000);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.SECONDS, queue);
        final Object lock = new Object();

        final Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (int i = 0; i < NN; i++) {
            map.put(i, 0);
        }
        long time = System.currentTimeMillis();
        for (int j = 0; j < NUMTIMES; j++) {
            for (int i = 0; i < NN; i++) {
                final int ii = i;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock) {
                            map.put(ii, map.get(ii) + 1);
                            sleep(10);

                            String res = "";
                            for (int k = 0; k < NN; k++) {
                                res += map.get(k) + ",";
                            }
                            System.out.println(res);
                            System.out.println("-----");
                        }
                    }
                });
            }
        }
        while (true) {
            if (executor.getCompletedTaskCount() == NUMTIMES * NN) {
                break;
            }
            sleep(1);
        }
        System.out.println("Synchronized millis: " + (System.currentTimeMillis() - time));
    }
}
