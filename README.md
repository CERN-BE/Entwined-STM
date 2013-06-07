Entwined-STM
============

Entwined is a Software Transactional Memory implementing collections with semantic concurrency control.

The following collections have been implemented so far:

-  `TransactionalMap` - interface similar to `java.util.Map` but with fewer functions
-  `TransactionalMultimap` - similar to Guava `Multimap`
-  `TransactionalQueue` - implements `java.util.Queue` interface
-  `TransactionalRef` - holds a single value.
-  `GlobalReference` - a special case of `TransactionalRef` that can be used as a field in any class.

Library is very mature and is at the core of [OASIS](http://project-oasis.web.cern.ch/project-oasis/) which is a mission critical system at CERN.

Examples
-------------
There are some examples in `src\test\java\cern\stm\demo`, but I'm planning to provide better examples in the near future. Stay tuned!


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

Apache 2

© Copyright 2013 CERN

  [Ivan Koblik]: koblik.blogspot.com
