(ns db.sharding
  (:require [db :as db]
            [utils :as u]
            [clojure.pprint :as pprint]
            [core :as sut]))



(comment

  (def ctx (atom (reset! sut/app-context (-> (assoc-in @sut/app-context [:config :app-env] "test")))))

  (def conn (u/connection ctx))

  (db/exec! ctx "drop table if exists news")

  (db/exec! ctx "drop table news_1")

  ;; Mother table
  (defn create-mother-table
   []
   (db/exec! ctx "CREATE TABLE if not exists news
                (id          bigint            not null,
                 category_id int               not null,
                 author      character varying not null,
                 rate        int               not null,
                 title       character varying)"))

   (create-mother-table)


  (defn generate-some-news []
    (doseq [news-num (range 0 100)
            :let [rate        (rand-int 5)
                  category_id (rand-int 5)]]
      (db/exec! ctx (format "insert into news (id, category_id, title, author, rate) values (%s, %s, 'My news %s', 'Ivan %s', %s)"
                            news-num category_id news-num news-num rate))))

  (generate-some-news)


  (db/exec! ctx (format "insert into news (id, category_id, title, author, rate) values (%s, %s, 'My news %s', 'Ivan %s', %s)"
                        1 1 1 1 1))

  (db/exec! ctx (format "insert into news (id, category_id, title, author, rate) values (%s, %s, 'My news %s', 'Ivan %s', %s)"
                        2 2 2 2 2))

  (db/exec! ctx (format "insert into news (id, category_id, title, author, rate) values (%s, %s, 'My news %s', 'Ivan %s', %s)"
                        3 3 3 3 3))

  (db/query ctx "select * from news where category_id = 1")

  (db/query ctx "select * from news_2")

  (db/query ctx "select * from news_1")

  (db/query ctx "select count(*) from news where category_id = 1")

  ;; Stopped worjing!

  (db/exec! ctx "drop table news cascade")


  (defn create-partition
    [ctx {:keys [table-name partition-name check]}]
    (db/exec! ctx (format "create table if not exists %s (check %s) inherits (%s);"
                          partition-name check table-name)))

  (defn create-partition-rule
    [ctx {:keys [table-name partition-name check]}]
    (db/exec! ctx (format "create or replace rule %s_%s as on insert to %s where %s DO INSTEAD INSERT INTO %s VALUES (NEW.*)"
                         table-name partition-name table-name check partition-name)))

  (defn create-partition-and-rule
    [ctx config]
    (do (create-partition      ctx config)
        (create-partition-rule ctx config)))

  (doseq [n (range 0 10)]
    (create-partition-and-rule
     ctx
     {:table-name      "news"
      :partition-name  (format "news_%s" n)
      :check           (format "(category_id = %s)" n)}))

  (db/query ctx "select * from news_1")

  (defn find-rules-for-table
     [ctx table-name]
     (db/query ctx
    (format "select n.*, d.*,
       c.relname as rule_table,
       case r.ev_type
         when '1' then 'SELECT'
         when '2' then 'UPDATE'
         when '3' then 'INSERT'
         when '4' then 'DELETE'
         else 'UNKNOWN'
       end as rule_event
  from pg_rewrite r
  join pg_class c on r.ev_class = c.oid
  left join pg_namespace n on n.oid = c.relnamespace
  left join pg_description d on r.oid = d.objoid
  where c.relname = '%s' " table-name)))

  (db/exec! ctx "delete from news")

  (clojure.pprint/pprint
   (find-rules-for-table ctx "news"))

  (db/exec! ctx "delete from pg_class where relname = 'news'")


  )
