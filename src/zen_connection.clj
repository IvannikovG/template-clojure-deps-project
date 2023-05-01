(ns zen-connection
  (:require [zen.core :as zen]
            [clojure.pprint :as pprint]
            [logging :as l]
            [utils :as u]
            [clojure.string :as str]))

(defn resource-exists?
  [ztx resource]
  (let [resource-symbol (->> resource :resourceType str str/lower-case (str "resources/") symbol)
        schema (zen/get-symbol ztx resource-symbol)]
    schema))

(defn validate
  [ctx resource]
  (if (seq (:resourceType resource))
    (let [dtx    (u/deatomize ctx)
          ztx    (:ztx dtx)
          schema (resource-exists? ztx resource)]
      (if schema
        (let [validated-resource (zen/validate-schema ztx schema resource)]
          (if (empty? (:errors validated-resource))
            resource
            {:errors (:errors validated-resource)
             :resource resource}))
        {:error (format "no schema found for resource: %s" (:resourceType resource))
         :resource resource}))
    {:error "No resourceType in resource"
     :resource resource}))


(defn find-resource-configs
  [ctx]
  (let [ztx                  (:ztx (u/deatomize ctx))
        resource-configs     (zen/get-tag ztx 'core/resource-config)
        find-valid-configs   (fn [configs] (remove (fn [c] (:errors c)) configs))
        find-invalid-configs (fn [configs] (filter (fn [c] (:errors c)) configs))
        configs-to-save      (->> resource-configs
                                  (reduce (fn [acc symbol]
                                            (let [resource-config   (zen/get-symbol ztx symbol)
                                                  validation-schema (zen/get-symbol ztx 'core/base-resource-config-schema)
                                                  validated-config  (zen/validate-schema ztx validation-schema resource-config)
                                                  config            (if (seq (:errors validated-config))
                                                                      {:errors       (:errors validated-config)
                                                                       :resourceType symbol}
                                                                      (-> resource-config
                                                                          (select-keys [:db-schema])
                                                                          (assoc :resourceType symbol)))]
                                              (conj acc config)))
                                          []))
        valid-configs        (find-valid-configs configs-to-save)
        invalid-configs      (find-invalid-configs configs-to-save)]
    {:valid-configs   valid-configs
     :invalid-configs invalid-configs}))


(defn handlers-edn->api
  [ctx]
  (let [ztx             (:ztx (u/deatomize ctx))
        handlers-names  (zen/get-tag ztx 'core/handler)
        api             (reduce (fn [acc el]
                                  (let [schema (->> (zen/get-symbol ztx el))
                                        ks     (get-in schema [:keys])
                                        name   (get ks :name)]
                                    (assoc acc name (dissoc ks :name))))
                                {}
                                handlers-names)]
    api))


(comment

  (def ctx core/app-context)

  (def test-db
    (get-in ctx [:db-connections :test-db]))

  test-db

  (db/query (assoc ctx :active-db-connection test-db) "SELECT * FROM pg_indexes")

  (keys ctx)
  (def ztx (zen/new-context {:unsafe true}))

  (zen/read-ns ztx 'core)

  (zen/read-ns ztx 'resources)

  (zen/read-ns ztx 'resource-configs)

  (def resources (zen/get-tag ztx 'core/resource-config))

  resources

  (def car (zen/get-tag ztx 'core/resource))

  car

  (def report-case (zen/get-symbol ztx 'resources/report-case))
  (def user (zen/get-symbol ztx 'resources/user))

  user

  (def car-config
    (zen/get-symbol ztx 'resource-configs/car-config))

  car-config

  (zen/validate-schema ztx car-config
                       {:db-schema
                        {:index-fields
                         [{:name "a" :path "a"}]}})

  (def resource-schemas
    (map (fn [resource]
           (zen/get-symbol ztx resource)) resources))

  resource-schemas
  (pprint/pprint resource-schemas)
  (zen/validate-schema ztx report-case
                       {:resourceType "report-case"
                        :id "id"
                        :text "as"
                        :car-registration-number "BK651X777"})

  ztx

  (let [ztx (get-in ctx [:zen])]
    ztx
    #_(->> (zen/get-tag ztx 'resources)))

  ( + 1 1 2 3)

  )
