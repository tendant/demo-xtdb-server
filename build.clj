(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'xtdb-node-server)
;; if you want a version of MAJOR.MINOR.COMMITS:
(def version (format "1.0.%s" (b/git-count-revs nil)))

(defn uber
  [opts]
  (-> opts
      (assoc :lib lib :main 'xtdb.server)
      (bb/clean)
      (bb/uber)))