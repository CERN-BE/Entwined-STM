(ns cern.entwined.core
  (:import [cern.entwined Memory 
            TransactionAdapter 
            CompositeCollection 
            SemiPersistent
            TransactionalMap
            TransactionalMultimap
            TransactionalQueue
            TransactionalRef]))

;;;; Construction helpers

(defn create-map [] (TransactionalMap.))
(defn create-multimap [] (TransactionalMultimap.))
(defn create-queue [] (TransactionalQueue.))
(defn create-ref [] (TransactionalRef.))

;;;; Entwined STM facade for Clojure

(defn create-root
  {:arglists '([:key1 ^Transactional value1 :key2 ^Transactional value2 & more])
   :doc "Creates root transactional object that is managed by the STM.
   Example: (create-root :map (create-map) :queue (create-queue))"}
  [& objects]
  (let [args (apply hash-map objects)
        ordered-keys (into [] (keys args))  ;absolute ordering of the keys
        key-to-index (into {} (map-indexed (fn [k v] [v k]) ordered-keys))
        make-storage (fn [values]
                       (CompositeCollection. (into-array SemiPersistent values)))
        make-proxy (fn make-proxy [^CompositeCollection storage]
                     (proxy [SemiPersistent clojure.lang.IDeref clojure.lang.ILookup] []
                       ;;SemiPersistent
                       (cleanCopy [] (make-proxy (.cleanCopy storage)))
                       (dirtyCopy [] (make-proxy (.dirtyCopy storage)))
                       (update [changes onlyReadLogs] (.update storage @changes))
                       (commit [global-state] (make-proxy (.commit storage @global-state)))
                       ;;IDeref
                       (deref [] storage)
                       ;;ILookup
                       (valAt [key & default] (if (key-to-index key) 
                                      (.get storage (key-to-index key))
                                      (first default)))))]
    (make-proxy (make-storage (map args ordered-keys)))))

(defn create-memory
  {:arglists '([:key1 ^Transactional value1 :key2 ^Transactional value2 & more])
   :doc "Constructs transactional memory from the given transactional entities.
   Example: (create-memory :map (create-map) :queue (create-queue))"}
  [& key-value-pairs]
  (Memory.  (apply create-root key-value-pairs)))

(defmacro intrans
  {:arglists '([^cern.entwined.Memory memory data & body])
   :doc "Runs a transaction in the Memory where body of the macro is executed in 
   the transaction. Symbol passed as second argument will be referencing the 
   argument of the Transaction.run method called by the Memory.
   Transaction commits only if body returns true or anything that evaluates to true."}
  ([^cern.entwined.Memory memory data & body]
    `(let [ret-val# (atom nil)]
       (.runTransaction ~memory 
         (proxy [TransactionAdapter] []
           (run [~data]
             (swap! ret-val# (fn [_#] (do ~@body)))
             (if (if (seq? @ret-val#) (doall @ret-val#) @ret-val#)
               true 
               false))))
       @ret-val#)))
