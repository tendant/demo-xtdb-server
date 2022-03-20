(ns node
  (:require [xtdb.api :as xt]
            [clojure.java.io :as io]
            [config.core :as config])
  (:import java.time.Duration))

(defn- mem-node-config
  []
  {})

;; enviroment varaibles:
;; XTDB_CHECKPOINTER_TYPE: "filesystem" or "s3"
;;
(defn- checkpointer-store
  []
  (case (:xtdb-checkpointer-type config/env)
    "filesystem" {:xtdb/module 'xtdb.checkpoint/->filesystem-checkpoint-store
                  :path (:xtdb-checkpointer-filesystem-path config/env)}
    "s3" {:xtdb/module 'xtdb.s3.checkpoint/->cp-store
          :bucket (:xtdb-checkpointer-s3-bucket config/env)
          :prefix (:xtdb-checkpointer-s3-prefix config/env)}))

(defn- jdbc-node-config []
  (let [config (cond-> {:xtdb.jdbc/connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                                                    :db-spec {:dbtype (:xtdb-jdbc-dbtype config/env)
                                                              :dbname (:xtdb-jdbc-dbname config/env)
                                                              :host (:xtdb-jdbc-host config/env)
                                                              :user (:xtdb-jdbc-user config/env)
                                                              :password (:xtdb-jdbc-password config/env)}
                                                    :pool-opts {:maximumPoolSize (or (:db-maximum-pool-size config/env) 2)}}
                        :xtdb/tx-log {:xtdb/module 'xtdb.jdbc/->tx-log
                                      :connection-pool :xtdb.jdbc/connection-pool}
                        :xtdb/document-store {:xtdb/module 'xtdb.jdbc/->document-store
                                              :connection-pool :xtdb.jdbc/connection-pool}
                        :xtdb/index-store {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                                                      :db-dir (io/file "./rocksdb")}}}
                 (:xtdb-checkpointer-enabled config/env) (assoc-in [:xtdb/index-store :kv-store :checkpointer]
                                                                    {:xtdb/module 'xtdb.checkpoint/->checkpointer
                                                                     :approx-frequency (Duration/ofMinutes (:xtdb-checkpointer-frequency-of-minutes config/env))
                                                                     :store (checkpointer-store)}))]
    config))

(defn- kafka-node-config []
  {:xtdb/index-store {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store, :db-dir "/var/lib/xtdb/indexes"}}
   :xtdb/document-store {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store, :db-dir "/var/lib/xtdb/documents"}}
   :xtdb/tx-log {:xtdb/module 'xtdb.kafka/->tx-log
                 :kafka-config {:bootstrap-servers "kafka:9092"}
                 }
   :xtdb.lucene/lucene-store {:db-dir "/var/lib/xtdb/lucene"}
   :xtdb.http-server/server {:port 3000
                             :jetty-opts {:host "0.0.0.0"}}})

(defn- node-config
  []
  (let [typed-config (case (:xtdb-node-type config/env)
                       "jdbc" (jdbc-node-config)
                       "mem" (mem-node-config)
                       "kafka" (kafka-node-config)
                       (mem-node-config))]
    (cond-> typed-config
      (:xtdb-checkpointer-enabled config/env) (assoc-in [:xtdb/index-store :kv-store :checkpointer]
                                                        {:xtdb/module 'xtdb.checkpoint/->checkpointer
                                                         :approx-frequency (Duration/ofMinutes (:xtdb-checkpointer-frequency-of-minutes config/env))
                                                         :store (checkpointer-store)}))))

(defn start-xtdb-node
  []
  (let [config (node-config)]
    (xt/start-node config)))
