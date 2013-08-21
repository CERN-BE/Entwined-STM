(ns cern.entwined.demo.clojure-benchmarks
  (:require [cern.entwined.core :as stm]))

(def pool (java.util.concurrent.Executors/newFixedThreadPool 11))
(defn schedule-update [update-fn] 
  (.execute pool update-fn))

(def start-latch (promise))
(def barrier (java.util.concurrent.CyclicBarrier. 11)) ;;10 threads in thread pool and one main thread

(defn make-update-fn [m key]
  (fn []
    @start-latch
    (doseq [x (range 1000)]
      (.put m key (inc (or (.get m key) 1))))
    (.await barrier)))

(defn run-concurrent-map []
  (let [m (java.util.concurrent.ConcurrentHashMap.)]
    (doseq [i (range 10)]
      (schedule-update (make-update-fn m (* i 13))))
    (deliver start-latch :ok)
    (.await barrier)
    m))

(defn make-trupdate-fn [memory key]
  (fn []
    @start-latch
    (doseq [x (range 1000)]
      (stm/intrans memory m
                   (.put m key (inc (or (.get m key) 1)))
                   true))
    (.await barrier)))

(defn run-transactional-map []
  (let [memory (cern.entwined.Memory. (stm/create-map))]
    (doseq [i (range 10)]
      (schedule-update (make-trupdate-fn memory (* i 13))))
    (deliver start-latch :ok)
    (.await barrier)
    (stm/intrans memory data (apply str (for [k (.keySet data)] [k (.get data k)])))))
