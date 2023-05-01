(ns db.sqlite
  (:require [dsql.pg]
            [zen-connection :as zc]
            [utils :as u]
            [sys   :as sys]))

(comment
  (def db
    {:classname "org.sqlite.JDBC"
     :subprotocol "sqlite"
     :subname "/Users/georgii/Library/Application Support/Google/Chrome/Default/History"})

  db



  (def sql-lite-db
    {:classname "org.sqlite.JDBC"
     :subprotocol "sqlite"
     :subname "sqlite/database.db"})


  (let [current-dir (sys/bash! "pwd" )
        _ (sys/bash! (format "mkdir sqlite"))\]
    current-dir)

  (def db* (assoc sql-lite-db
                 :connection (jdbc/get-connection sql-lite-db))))
