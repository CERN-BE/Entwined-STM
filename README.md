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

Examples
-------------
You can find examples in [src/test/java/cern/entwined/demo](src/test/java/cern/entwined/demo). Here I list some of them.
- [SingleMapDemo](src/test/java/cern/entwined/demo/SingleMapDemo.java) quickly demonstrates how to use signle TransactionalMap.
- [IsolationDemo](src/test/java/cern/entwined/demo/IsolationDemo.java) demonstrates that local changes in a transaction are not visible to concurrent transactions.
- [RollbackDemo](src/test/java/cern/entwined/demo/RollbackDemo.java) shows that if transaction is terminated by an exception any local changes are discarded.


Installation
--------------

I'm planning to publish it on Maven central, but for now you can build it from the sources.

```
git clone git://github.com/CERN-BE/Entwined-STM.git
cd Entwined-STM
mvn jar:jar
ls -l target
```

License
-

© Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.

  [Ivan Koblik]: koblik.blogspot.com
