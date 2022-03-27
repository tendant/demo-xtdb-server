(ns server
  (:require [taoensso.timbre :as log]
            [node :as node]
            [ring.adapter.jetty :as j]
            [reitit.ring :as rr]
            [xtdb.http-server :as xtdb-http-server])
  (:import
   java.io.OutputStream))

(log/merge-config!
 {:min-level [["taoensso.*" :error]
              ["crux.tx" :debug]
              ["crux.*" :info]
              ["*" :info]]
  :ns-filter #{"*"} #_{:deny #{"taoensso.*"} :allow #{"*"}}

  :middleware [] ; (fns [appender-data]) -> ?data, applied left->right

  :appenders {:println (log/println-appender {:stream :auto})}})

(extend-protocol ring.core.protocols/StreamableResponseBody
  (Class/forName "[B")
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (with-open [out output-stream]
      (.write out ^bytes body)))

  juxt.clojars_mirrors.muuntaja.v0v6v8.muuntaja.protocols.StreamableResponse
  (write-body-to-stream [this _ ^OutputStream output-stream]
    (with-open [out output-stream]
      ((.f this) ^OutputStream out))))

(defn string->stream
  ([s] (string->stream s "UTF-8"))
  ([s encoding]
   (-> s
       (.getBytes encoding)
       (java.io.ByteArrayInputStream.))))

(defn wrap-debug
  [handler]
  (fn [req]
    (println "\nREQ:" req)
    (let [req-body (slurp (:body req))
          _ (println "REQ body:" req-body)
          new-req (assoc req :body (string->stream req-body))
          resp (handler new-req)
          body (slurp (:body resp))]
      (println "RESP:" (dissoc resp :body))
      (println "Body:" body)
      (assoc resp :body (string->stream body)))))

(defn create-crux-app
  []
  (-> (rr/routes
       (xtdb-http-server/->xtdb-handler (node/start-xtdb-node) {}))
      (wrap-debug)))

(defn -main
  [& args]
  (let [port 3000]
    (log/info "Starting server. port:" port)
    (j/run-jetty (create-crux-app)
                 {:port port})
    (log/info "Server started on port:" port)))
