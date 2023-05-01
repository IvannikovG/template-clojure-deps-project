(ns db
  (:require [dsql.pg]
            [clojure.java.jdbc :as jdbc]
            #_[next.jdbc :as jdbc]
            #_[next.jdbc.sql :as sql]
            [db-extensions]
            [clojure.data.json :as json]
            [clojure.string :as str]
            #_[next.jdbc.result-set :as rs]
            [logging :as l]
            [zen.core :as zen]
            [clojure.pprint :as pprint]
            [zen-connection :as zc]
            [honey.sql :as hsql]
            [honey.sql.helpers :as honh]
            [utils :as u]))

(defn sn [x] (if (keyword? x) (name x) (str x)))

(defn stringable? [v]
  (or (string? v) (keyword? v)))

(defn add-func
  [func raw]
  (let [sql-text (-> raw first second)]
    [[:raw (str "(" sql-text ")" "::" (str func))]]))

(defn h>>
  [col path]
  [[:raw (str (-> col name str) "#>>" "'{" (str/join "," (mapv sn path)) "}'")]])

(defn h>
  [col path]
  [[:raw (str (-> col name str) "#>" "'{" (str/join "," (mapv sn path)) "}'")]])

(defn quote-wrap [s]
  (str "'" s "'"))

(defn fmt
  [q]
  (cond (:dsql q)
        (dsql.pg/format (:dsql q))
        (:h q)
        (hsql/format (:h q))
        :else q))

(defn safe-format
  [arg]
  (if (vector? arg)
    arg
    (vector arg)))

#_(defn query [ctx q]
  (let [connection (u/connection ctx)]
    (let [to-query (cond (:dsql q) (safe-format (dsql.pg/format (:dsql q)))
                         (:h q)    (safe-format (hsql/format (:h q)))
                         :else     (safe-format q))]
      (jdbc/execute! connection to-query {:return-keys true :builder-fn rs/as-unqualified-lower-maps}))))

(defn query [ctx q]
  (let [connection (u/connection ctx)
        query-result (cond (:dsql q)
                           (jdbc/query connection (dsql.pg/format (:dsql q)))
                           (:h q)
                           (jdbc/query connection (hsql/format (:h q)))
                           :else (jdbc/query connection q))]
    query-result))

(defn query-first [ctx q]
  (first (query ctx q)))

(defn jsonify-resource
    [resource]
    (let [resource-string (json/write-str resource)]
      resource-string))

(defn safe-name
  [name]
  (if (empty? (str name))
    (prn "Error in safe-name")
    (-> name
        str
        (str/lower-case)
        (str/replace #"-" "_")
        (str/replace #"resources/" ""))))

#_(defn exec! [ctx q]
  (let [connection (u/connection ctx)]
    (let [to-query (cond (:dsql q) (safe-format (dsql.pg/format (:dsql q)))
                         (:h q)    (safe-format (hsql/format (:dsql q)))
                         :else     (safe-format q))]
      (jdbc/execute! connection to-query))))

(defn exec! [ctx q]
  (let [connection (u/connection ctx)]
    (let [query-result (cond (:dsql q)
                             (jdbc/execute! connection (dsql.pg/format (:dsql q)))
                             (:h q)
                             (jdbc/execute! connection (hsql/format (:dsql q)))
                             :else (jdbc/execute! connection q))]
      query-result)))

#_(defn save!
  [ctx resource]
  (let [connection            (u/connection ctx)
        resource-type         (get resource :resourceType)
        #_#__                     (assert (seq resource-type) (println "CANT SAVE A RESOURCE WITH NO RESOURCETYPE: " resource))
        table                 (-> resource-type safe-name)

        insert-query          (fmt {:h (-> (honh/insert-into (keyword table))
                                           (honh/columns :id :cts :resource)
                                           (honh/values [[(u/uuid) (u/sql-now) [:lift resource]]])
                                           (honh/upsert (-> (honh/on-conflict :id)
                                                            (honh/do-update-set :resource))))})]
    (if (seq resource-type)
      (jdbc/execute-one! connection (safe-format insert-query)
                         {:return-keys true :builder-fn rs/as-unqualified-lower-maps})
      {:error "resourceType required"})))


(defn delete-record [ctx table where]
  (jdbc/delete! ctx table where))

(defn save!
  [ctx resource]
  (let [connection            (u/connection ctx)
        resource-type         (get resource :resourceType)
        table                 (-> resource-type safe-name)

        resource-id           (:id resource)
        resource-with-same-id (when resource-id
                                (query-first
                                 ctx
                                 (format "select * from %s where resource#>>'{id}' = '%s'::text" table resource-id)))]

    (if (seq resource-type)
      (first (if (seq resource-with-same-id)
               (into {} (jdbc/update! connection (keyword table) {:cts (u/sql-now)
                                                                  :resource (assoc resource :id resource-id)}
                                     ["resource#>>'{id}' = ?" resource-id]
                                     {:return-keys ["resource" "id"]}))
               (into {} (let [id (u/uuid)]
                          (jdbc/insert! connection (keyword table) {:id id
                                                                    :ts (u/sql-now)
                                                                    :resource (assoc resource :id id)}
                                        {:return-keys ["resource" "id"]})))))
      {:error "resourceType required"})))


(defn save-resource!
  [ctx resource]
  (let [dctx (u/deatomize ctx)
        errors (:errors (zc/validate dctx resource))
        _ (when errors
            (do (println "ERRORS, saving resource" errors)
                (pprint/pprint resource)))]
    (if (seq errors)
      (throw (Exception. "resource invalid"))
      (save! dctx resource))))


(defn table-and-trigger-creation-query
  [{:keys [table-name resource-type if-not-exists]}]
  (format "create table %s %s
           (id            text            primary key not null,
            cts           timestamp       with time zone default current_timestamp,
            ts            timestamp       with time zone default current_timestamp,
            resource_type text            default '%s'::text,
            resource      jsonb           not null);

           DROP TRIGGER IF EXISTS %s_ts_trigger on %s;

           CREATE TRIGGER %s_ts_trigger
           BEFORE UPDATE ON %s
           FOR EACH ROW
           EXECUTE FUNCTION update_ts_column();
"
          (if if-not-exists "if not exists" "")
          table-name resource-type table-name
          table-name table-name table-name))

(defn update-func-creation-query
  []
"CREATE OR REPLACE FUNCTION update_ts_column()
 RETURNS TRIGGER AS $$
 BEGIN
    NEW.ts = now();
    RETURN NEW;
 END;
 $$ LANGUAGE plpgsql;")

(defn create-core-tables [ctx]
  (let [ztx (:ztx (u/deatomize ctx))
        _ (zen/read-ns ztx 'resources)
        resources (zen/get-tag ztx 'core/resource)

        resource-schemas (mapv (fn [resource]
                                 (zen/get-symbol ztx resource)) resources)]
    (exec! ctx (update-func-creation-query))
    (doseq [{zen-name :zen/name _keys :keys :as _resource} resource-schemas]
      (let [table-name (safe-name zen-name)
            table-creation-query (table-and-trigger-creation-query {:table-name table-name
                                                                    :resource-type table-name
                                                                    :if-not-exists true})
            #_#_trigger-query        (create-trigger-query table-name)]
        (l/easy-logger {:process "Table creator"
                        :message (format "Creating table: %s;" table-name)})
        (do
          (exec! ctx table-creation-query))))))

(defn safe-table-name
  [resourceType]
  (-> resourceType
      (str/replace #"resource-configs/" "")
      (str/replace #"-config" "")
      (str/replace #"-" "_")))

(defn safe-index-name
  [name]
  (-> name
      (str/replace #"-" "_")))

(defn create-index-query
  [resourceType {:keys [name path] :as _index-field}]
  (let [table-name (safe-table-name resourceType)
        index-name (safe-index-name name)]
    (format "CREATE INDEX IF NOT EXISTS %s ON %s (%s)" index-name table-name path)))

(defn process-resource-configs
  [ctx {:keys [valid-configs invalid-configs] :as _resource-configs}]

  (doseq [invalid-config invalid-configs]
    (println (format "Found an invalid resource-config: %s. Skipping" invalid-config)))

  (doseq [{:keys [resourceType db-schema]} valid-configs
          :let [index-fields (get db-schema :index-fields)]]
    (doseq [index-field index-fields
            :let [index-query (create-index-query resourceType index-field)]]
      (exec! ctx index-query))))

(defn find-and-process-resource-configs
  [ctx]
  (let [resource-configs (zc/find-resource-configs ctx)]
    (process-resource-configs ctx resource-configs)))

(defn already-run-migration?
  [ctx {:keys [name _sql date] :as _migration}]
  (let [current-migration-version
        (query ctx
               (format "select * from migration
                        where resource#>>'{name}' = '%s'
                        and   (resource#>>'{date}')::date <= '%s'::date;"
                       name date))
        already-run? (if (empty? current-migration-version)
                       false
                       true)]
    already-run?))


(defn migrations-to-run
  [ctx migrations]
  (->> migrations
       (filter (fn [m] (:active m)))
       (remove (fn [m] (already-run-migration? ctx m)))
       (sort-by :date)))

(defn run-migrations
  [ctx]
  (let [{:keys [migrations]}   (u/deatomize ctx)
        migrations-to-run!     (migrations-to-run ctx migrations)
        migration-insert-query (fn [{:keys [name date _sql] :as _migration}]
                                 (format "insert into migration (id, resource_type, resource)
                                          values
                                        ('%s', 'migration', '{\"name\": \"%s\",
                                                              \"date\": \"%s\"}'::jsonb);"
                                         (u/uuid) name date))]
    (when (seq migrations-to-run!)
      (doseq [m migrations-to-run!]
        (exec! ctx (migration-insert-query m))))

    (when (seq migrations-to-run!)
        (doseq [m migrations-to-run!]
           (exec! ctx (:sql m))))))

(defn run-global-migrations
  [ctx ]
  (let [{:keys [global-migrations]} (u/deatomize ctx)]
    (doseq [m global-migrations]
      (exec! ctx (:sql m)))))

(defn run-system-migrations
  [ctx]
  (let []
    (println "Running migrations" )
    (run-global-migrations ctx)
    (run-migrations ctx)))

(comment

  (def test-ctx
    (let [ctx context/general-context
          new-api (->> (-> ctx :api)
                       (reduce-kv (fn [acc k v]
                                    (if (and (map? v)
                                             (not (-> v :need-auth!)))
                                      (assoc acc k (dissoc v :login-required?))
                                      (assoc acc k v)))
                                  {}))
          ctx* (-> ctx
                   (assoc-in [:config :app-env] "test")
                   (assoc-in [:api] new-api)
                   (assoc-in [:active-db-connection] (utils/test-ds)))]
      ctx*))

  (def query
    {:dsql [:pg/sql (format "select %s;" "1; select 2")]})

  (jdbc/execute! (:active-db-connection (u/deatomize u/dev-ctx)) ["select 1;"])

  (db/query (u/deatomize u/dev-ctx) ["select 1;"])

  (create-core-tables core/app-context)

  (query core/app-context
         "select * from test")

  (exec! core/app-context
         "insert into test (id, resource)
          values (2, '{\"a\": \"1\",
                       \"c\": {\"c2\": [\"1\", \"2\", \"3\"] } }')")


  (query core/app-context "select * from pg_indexes where schemaname = 'public'")

  (def resource
    {:resourceType "test2"
     :id "12312"
     :resource {:a 1
                :b [1 2 3]}})

  (save! core/app-context
         resource)

  (json/write-str {:a 1 {:a 1} 1})

  (def resource
    {:resourceType "test2"
     :id "12312"
     :resource {:a 1
                :b [1 2 3]}})

  (def resource
    {:resourceType "test2"
     :id "12312"
     :resource {:a 1
                :b [1 2 3]}})

  (query (u/connection core/app-context)
         "select * from test2")


  )
