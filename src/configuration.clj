(ns configuration
  (:require [db :as db]
            [zen.core :as zen]
            [zen-connection :as zc]
            #_[next.jdbc.connection :as conn]
            [utils :as utils]
            #_[next.jdbc :as jdbc :refer [get-datasource]])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def test-ds utils/test-ds)

(defn general-initialize [ctx]
  (println "General initialization")
  (when (= (System/getenv "RELEASE") "rel")
    (prn "Initializing with envs: "
         {:pghost (System/getenv "PGHOST")
          :pgport (System/getenv "PGPORT")}))

  (let [ztx          (zen/new-context {:unsafe true})
        zen-ns-names (get-in ctx [:zen-resources])
        _init-ztx    (doseq [ns-name zen-ns-names]
                       (zen/read-ns ztx ns-name))
        ctx*         (-> ctx
                         (assoc-in [:ztx] ztx))
        api          (zc/handlers-edn->api ctx*)
        ctx**        (-> ctx*
                         (assoc-in [:api] api))]
    (println "Initialized zen context to :ztx" (keys ctx**))
    (db/create-core-tables ctx**)
    (db/run-system-migrations ctx**)
    (db/find-and-process-resource-configs ctx**)
    (prn "Finished general init: " (keys ctx**))
    ctx**))

(defn test-initialize
  [ctx]
  (let []
    (println "End test app initialization")
    ctx))

(defn initialize
  [ctx]
  (prn "Initializing dev..")
  ctx)

(defn initialize-with-env
  [ctx]
  (let [app-env (get-in ctx [:config :app-env])
        ctx*    (general-initialize ctx)
        ctx**   (cond (= app-env "test")    (test-initialize ctx*)
                      (not= app-env "test") (initialize      ctx*)
                      :else                 (println "Can't initialize app"))]
    (println "Initialized: " app-env (keys ctx**))
    ctx**))

(defn init
  [ctx]
  (initialize-with-env ctx))

(comment

  (jdbc/execute! (utils/test-ds) ["select 1;"])

  (with-open [^HikariDataSource pool-ds
              (conn/->pool HikariDataSource utils/dev-db-spec)]
    (.close (jdbc/get-connection pool-ds))


    (prn (jdbc/execute! pool-ds ["select 1;"]))
    (def a (atom pool-ds))

    (prn (jdbc/execute! @a ["select 1;"]))

    #_(jdbc/execute! ds "select 1;"))



  )
