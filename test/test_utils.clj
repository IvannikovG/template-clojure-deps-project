(ns test-utils
  (:require [context :as context]
            [core :as sut]
            [configuration :as c]
            [zen.core :as zen]
            [utils :as utils]
            [db :as db]))

(def test-ctx
  (let [ctx     (-> context/general-context
                    (assoc-in [:active-db-connection] (utils/test-ds))
                    (assoc-in [:config :app-env] "test"))
        ctx*    (c/general-initialize ctx)

        new-api (->> (-> ctx* :api)
                     (reduce-kv (fn [acc k v]
                                  (if (and (map? v)
                                           (not (-> v :need-auth!)))
                                    (assoc acc k (dissoc v :login-required?))
                                    (assoc acc k v)))
                                {}))

        ctx** (-> ctx*
                  (assoc-in [:api] new-api))]
    (prn "KEYS:: " (keys ctx*) (keys ctx**))
    ctx**))

(comment

  (dissoc test-ctx :ztx)

  )

(defn test-app
  []
  (let [ctx test-ctx]
    (sut/test-app ctx)))

(defn test-query
  [query]
  (db/query test-ctx query))

(defn test-query-first
  [query]
  (first (test-query query)))

(defn test-exec!
  [query]
  (db/exec! test-ctx query))

(defn test-save!
  [query]
  (db/save! test-ctx query))
