(ns xtdb-meetup.core
  (:require
    [clojure.java.io :as io]
    [xtdb.api :as xt]))

(declare xtdb-node)

(defn start-xtdb!
  []
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store,
                        :db-dir      (io/file dir),
                        :sync?       true}})]
    (xt/start-node
      {:xtdb/tx-log         (kv-store "data/dev/tx-log"),
       :xtdb/document-store (kv-store "data/dev/document-store"),
       :xtdb/index-store    (kv-store "data/dev/index-store")})))

(defn stop-xtdb! []
  (.close xtdb-node))

(comment

  (def xtdb-node (start-xtdb!))
  
  (stop-xtdb!)

  )
 
  


  
  
