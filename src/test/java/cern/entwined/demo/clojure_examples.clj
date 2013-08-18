(ns cern.entwined.demo.clojure-examples
  (:require [cern.entwined.core :as stm]))

(def memory (stm/create-memory :users (stm/create-map) :tasks (stm/create-queue)))

(defn add-user [id user]
  (stm/intrans memory data
               (-> data :users (.put id user))
               true))

(defn get-user [id]
  (stm/intrans memory data
               (-> data :users (.get id))))

(defn schedule-task [task-fn]
  (stm/intrans memory data
               (-> data :tasks (.offer task-fn))))

(defn drain-tasks []
  (stm/intrans memory data
               (let [result (java.util.ArrayList.)]
                 (-> data :tasks (.drainTo result))
                 result)))
