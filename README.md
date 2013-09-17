Entwined-STM  [![Build Status](https://travis-ci.org/CERN-BE/Entwined-STM.png?branch=master)](https://travis-ci.org/CERN-BE/Entwined-STM)
============

Entwined is a Software Transactional Memory implementing collections with semantic concurrency control. Library provides ACI (Atomicity, Consistency, Isolation) guarantees with closed nested transactions.

The following collections have been implemented so far:

-  `TransactionalMap` - interface similar to `java.util.Map` but with fewer functions
-  `TransactionalMultimap` - similar to Guava `Multimap`
-  `TransactionalQueue` - implements `java.util.Queue` interface
-  `TransactionalRef` - holds a single value.
-  `GlobalReference` - a special case of `TransactionalRef` that can be used as a field in any class.

Library is very mature and is at the core of [OASIS](http://project-oasis.web.cern.ch/project-oasis/) which is a mission critical system at CERN.

Overview
-------------

Entry point to the library is the [Memory](src/main/java/cern/entwined/Memory.java) class. To construct it client has to provide an instance of the [SemiPersistent](src/main/java/cern/entwined/SemiPersistent.java) abstract class. All the transactional collections listed above extend this class.

The client can define her own transactional data type by directly extending SemiPersistent class or by using [CompositeCollection](src/main/java/cern/entwined/CompositeCollection.java). [TestSnapshot](src/test/java/cern/entwined/TestSnapshot.java) is a good example of a custom implementation.

To access the memory client has to run transaction:

```java
Memory<TransactionalMap<Integer, Integer>> memory = new Memory<TransactionalMap<Integer, Integer>>(
                new TransactionalMap<Integer, Integer>());
memory.runTransaction(new Transaction<TransactionalMap<Integer, Integer>>() {
    @Override
    public boolean run(TransactionalMap<Integer, Integer> data) throws Exception {
        //Operate on data here.

        return true; //true to commit, false to rollback
    }

    @Override
    public void committed(TransactionalMap<Integer, Integer> data) throws Exception {
        //Called when transaction has been committed. Commit callbacks are globally ordered
        //and are called in the commit order.

        //Read data here, any changes will be discarded.
    }
}
```

During commit memory may detect conflicts due to concurrent changes. In such a case `Memory` class will restart the transaction by discarding all the modifications done in the `run` method and by calling it again with the fresh snapshot of the data. Because of this it is crucial to never perform output in the `run` method, any output operation should be reserved for the `committed` block.

Examples
-------------
You can find examples in [src/test/java/cern/entwined/demo](src/test/java/cern/entwined/demo). Here I list some of them.
- [SingleMapDemo](src/test/java/cern/entwined/demo/SingleMapDemo.java) quickly demonstrates how to use signle TransactionalMap.
- [IsolationDemo](src/test/java/cern/entwined/demo/IsolationDemo.java) demonstrates that local changes in a transaction are not visible to concurrent transactions.
- [RollbackDemo](src/test/java/cern/entwined/demo/RollbackDemo.java) shows that if transaction is terminated by an exception any local changes are discarded.
- [Clojure examples](src/test/java/cern/entwined/demo/clojure_examples.clj) makes use of Clojure facade. Shows how to crate a custom snapshot and access it.


Installation
--------------

For Maven users please add this dependency to your `pom.xml`:

```xml
<dependency>
	<groupId>ch.cern</groupId>
	<artifactId>entwined-stm</artifactId>
	<version>1.0.1</version>
</dependency>
```

For Leiningen users please add this line to your `project.clj`:

```clojure
[ch.cern/entwined-stm "1.0.1"]
```


License
-

© Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.

  [Ivan Koblik]: koblik.blogspot.com
