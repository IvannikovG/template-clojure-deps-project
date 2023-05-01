(ns core
  (:require [ring.middleware.reload :refer [wrap-reload]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.cookies :refer [wrap-cookies cookies-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [jumblerg.middleware.cors :refer [wrap-cors]]
            [logging :as log]
            [configuration :as configuration]
            [routing :as routing]
            [auth.login :as login]
            [auth.register :as register]
            [handlers :as h]
            #_#_[next.jdbc.connection :as conn]
            [next.jdbc :as jdbc :refer [get-datasource]]
            [db :as db]
            [context :as context]
            [utils :as u]
            [connection-pool :as cp]
            [user-handlers.handlers :as uh]
            [zen.core :as zen]
            [clojure.string :as str])
  #_(:import (com.zaxxer.hikari HikariDataSource))
  (:gen-class))


(def app-context context/general-context)

(defn initialize-app [ctx]
  (configuration/init ctx))

(defn router
  "Routing here"
  [ctx request]
  (routing/routing-mapper ctx request))

(defn test-app
  [ctx]
  (println "Initialized test app")
  (fn [request]
    (router ctx request)))

(defn bare-app
  [ctx]
  (fn [request]
    (let [uri (:uri request)]
      (when (and
             (= (System/getenv "RELEASE") "rel")
             (not (str/includes? uri "static")))
        (log/easy-logger "request-logging: "
                         (select-keys request [:uri :query-string :query-params
                                               :server-port])))
      (router ctx request))))

(defn reloadable-app [ctx]
  (if (= (System/getenv "DEVELOPMENT") "development")
    (wrap-reload (bare-app ctx))
    (bare-app ctx)))

#_(defn custom-middlewares
  [function]
  (fn [& request]
    (apply function request)))

(defn app [ctx]
  (-> (reloadable-app ctx)
      #_custom-middlewares
      wrap-params
      wrap-cookies
      wrap-keyword-params
      (wrap-json-response {:keywords? true})
      (wrap-json-body {:keywords? true})
      (wrap-cors #".*")))

(defn -serve [ctx port]
  (let [ctx* (initialize-app ctx)]
    (run-jetty (app ctx*)
               {:port port :join? false})))

(defonce srv (atom nil))

(defn start-server [ctx]
  (reset! srv (-serve ctx 7003)))

(defn stop-server []
  (when-some [s @srv]
    (.stop s)
    (reset! srv nil)))


(defn -main [& _args]
  (let [ctx     app-context
        dctx    (utils/deatomize ctx)
        env     (-> dctx :config :app-env)
        test?   (= env "test")
        dev?    (= env "dev")
        db-conn (cond dev? utils/dev-db-spec2
                      #_cp/datasource
                      #_(cp/ensure-pool (cp/dev-pool))
                      #_(utils/dev-ds)
                      test? utils/test-ds
                      :else {})
        ctx* (-> dctx (assoc-in [:active-db-connection] db-conn))]
    (start-server ctx*)))

(comment
  (def a (jdbc/execute! (cp/dev-pool) ["select 1"]))

  (str/includes? "/static/css/icons/sorted_desc.svg" "static")

  (-main)

  (stop-server)
  
)
