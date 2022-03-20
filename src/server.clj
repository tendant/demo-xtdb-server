(ns server
  (:require [taoensso.timbre :as log]
            [node :as node]
            [ring.adapter.jetty :as j]
            [reitit.ring :as rr]
            [xtdb.http-server :as xtdb-http-server]))

(log/merge-config!
 {:min-level [["taoensso.*" :error]
              ["crux.tx" :debug]
              ["crux.*" :info]
              ["*" :info]]
  :ns-filter #{"*"} #_{:deny #{"taoensso.*"} :allow #{"*"}}

  :middleware [] ; (fns [appender-data]) -> ?data, applied left->right

  :appenders {:println (log/println-appender {:stream :auto})}})

(defn create-crux-app
  []
  (rr/routes
   (xtdb-http-server/->xtdb-handler (node/start-xtdb-node) {})))

(defn -main
  [& args]
  (let [port 8080]
    (log/info "Starting server. port:" port)
    (j/run-jetty (create-crux-app)
                 {:port port})
    (log/info "Server started on port:" port)))
