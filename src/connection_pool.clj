(ns connection-pool
  (:require #_[next.jdbc :as jdbc]
            [utils :as u]
            [clojure.java.jdbc :as jdbc]
            [db :as db]
            [clojure.string :as str]
            #_[next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource HikariConfig)
           (java.util Properties)))

(defn- create-datasource [db-spec]
  (let [config (doto (HikariConfig.)
                 (.setJdbcUrl (:jdbc-url db-spec))
                 (.setUsername (:user db-spec))
                 (.setPassword (:password db-spec))
                 (.setDriverClassName "org.postgresql.Driver")
                 (.setMaximumPoolSize (:max-pool-size db-spec 10))
                 (.setConnectionTimeout (:connection-timeout-ms db-spec 30000))
                 (.setIdleTimeout (:idle-timeout-ms db-spec 600000))
                 (.setMinimumIdle (:min-idle db-spec 1))
                 (.addDataSourceProperty "user" (:user db-spec))
                 (.addDataSourceProperty "password" (:password db-spec))
                 (.setConnectionTestQuery nil))]
    {:datasource (HikariDataSource. config)}))

(defn build-postgresql-url
  [{:keys [host port databaseName user password]}]
  (str "jdbc:postgresql://" host ":" port "/" databaseName
       "?user=" user "&password=" password))

(def jdbc-url (build-postgresql-url u/db-spec))

(def datasource (delay (create-datasource
                        (assoc u/db-spec :jdbc-url jdbc-url) )))

(comment

  (jdbc/query @datasource ["select 1;"])

  (jdbc/insert! @datasource :chinesegrapheme
                {:id (u/uuid)
                 :resource {:hieroglyph_id 1}})

  )

#_(def defaults
  {:auto-commit        true
   :read-only          false
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       60000
   :max-lifetime       180000
   :minimum-idle       10
   :maximum-pool-size  10
   :connection-init-sql "select 1"})

#_(defn upcase [^String s]
  (str
   (.toUpperCase (.substring s 0 1))
   (.substring s 1)))

#_(defn propertize [k]
  (let [parts (str/split (name k) #"-")]
    (str (first parts) (str/join "" (map upcase (rest parts))))))

#_(defn create-pool [opts]
  (let [props (Properties.)]
    #_(.setProperty props "dataSourceClassName" "org.postgresql.ds.PGSimpleDataSource")
    (doseq [[k v] (merge defaults opts)]
      (when (and k v)
        (.setProperty props (propertize k) (str v))))
    (-> props
        HikariConfig.
        HikariDataSource.)))

#_(defn dev-pool
  []
  (create-pool (merge utils/dev-db-spec
                      defaults)))

#_(defn ensure-pool
  [pool]
  (let [^HikariDataSource ds pool
        test-res (jdbc/execute! ds ["select 'initialzed hikari pool' as pool_greeting;"])]
    (prn "-=====================-")
    (prn test-res)
    (prn "-=====================-")
    ds))

#_(defn new-cp
  []
  (let [^HikariDataSource ds
        (connection/->pool com.zaxxer.hikari.HikariDataSource
                           {:dbtype "postgres" :dbname "chinese_dictionary_db"
                            :username "postgres" :password "postgres"
                            :port 5434
                            :dataSourceProperties {:socketTimeout 30}})]
    ds))

(comment

  (jdbc/insert! connection (keyword table) {:id (u/uuid)
                                            :resource resource})

  )

(comment
  (def apool (dev-pool))

  (def apool2 (dev-pool))

  (.close (jdbc/get-connection apool))

  (doseq [_ (range 1000)]
    (jdbc/execute! new-cp ["select 1;"] ))

  (db/exec! {:active-db-connection apool
             :config {:app-env "dev"}} ["select 1;"])

  )
#_(jdbc/execute! (jdbc/get-connection dds) ["select 1;"])
