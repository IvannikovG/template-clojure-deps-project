(ns handler-helpers
  (:require [clojure.string :as str]
            [clj-jwt.core  :as jwt]))

(defn parse-config-values
  [config-key config-value]
  (let [paths-regexp #"(?<=\<).+?(?=\>)"
        dictionaries (re-seq paths-regexp config-value)]
    (cond (or (= config-key "paths") (= config-key "where"))
          (reduce (fn [acc dictionary]
                    (let [[key vals] (str/split dictionary #":")
                          values     (str/split vals #",")
                          clean-key  (str/split key #",")]
                      (assoc acc clean-key values)))
                  {}
                  dictionaries)
          (or (= config-key "group-by")
              (= config-key "order-by")) (mapv (fn [el] (str/split el #",")) dictionaries)
          :else (str/split config-value #","))))


(defn parse-query-string
  [query-string]
  (let [[resource configuration-string] (str/split query-string #"\?")
        config (reduce
                (fn [acc el]
                  (let [[config-key config-value] (str/split el #"=")
                        values (parse-config-values config-key config-value)]
                    (assoc acc (keyword config-key) values)))
                {}
                (str/split configuration-string #"&"))]
    {:resource resource
     :config config}))


(defn process-as-paths
  [paths]
  (reduce-kv (fn [acc name selector]
               (let [comma            (if (seq acc) ", " " ")
                     resource-payload (str/join "," selector)
                     resource         (if (= resource-payload "*")
                                        resource-payload
                                        (str "resource#>>'{" resource-payload "}'"))
                     as       (if (= resource-payload "*")
                                ""
                                (str " AS "(str/join "_" name) " "))]
                 (str acc comma resource as)))
             ""
             paths))


(defn process-as-where
  [where]
  (reduce-kv (fn [acc selector params]
               (let [or (if (seq acc) "OR " " ")
                     resource (str "resource#>>'{" (str/join "," selector) "}'")
                     in       (str " IN " "(" (->> params
                                                   (map (fn [param] (format "'%s'" param)))
                                                   (str/join ",")) ")" " ")]
                 (str acc or resource in)))
             ""
             where))

(defn process-as-aggregate
  [aggregate]
  (reduce-kv (fn [acc id params]
               (let [comma    (if (seq acc) ", " " ")
                     resource (str "resource#>>'{" (str/join "," params) "}'")]
                 (str acc comma resource)))
             ""
             aggregate))


(defn config->dsql
  [{:keys [paths _limit _offset group-by order-by where resource] :as _config}]
  (let [select        (process-as-paths paths)
        proc-as-whr   (process-as-where where)
        safe-where    (when (seq proc-as-whr)
                        [:pg/sql proc-as-whr])
        order-by      (process-as-aggregate order-by)
        group-by      (process-as-aggregate group-by)
        safe-group-by (if (seq group-by)
                        [:pg/sql group-by]
                        nil)
        safe-order-by (if (seq order-by)
                        [:pg/sql order-by]
                        nil)]
    {:select   [:pg/sql select]
     :from     (-> resource
                   (str/replace #"resource" "")
                   (str/replace #"/" "")
                   keyword)
     :where    safe-where
     :group-by safe-group-by
     :limit    (read-string (first _limit))
     :offset   (read-string (first _offset))
     :order-by safe-order-by}))


(defn build-sql-from-query-config
  [{:keys [resource config]}]
  (let [add-default-path   (fn [config] (if (not (:paths config))
                                          (assoc config :paths {["all"] ["*"]})
                                          config))
        add-default-limit  (fn [config] (if (not (:_limit config))
                                          (assoc config :_limit ["50"])
                                          config))
        add-default-offset (fn [config] (if (not (:_offset config))
                                          (assoc config :_offset ["0"])
                                          config))
        processed-config   (-> config
                               add-default-limit
                               add-default-offset
                               add-default-path
                               (select-keys
                                [:_limit :_offset :paths :group-by :where :order-by])
                               (assoc :resource resource))]
    {:dsql (config->dsql processed-config)}))

(defn jwt-access-token
  [headers cookies]
  (let [token-from-headers (-> (get-in headers ["authorization"])
                               str
                               (str/split #" ")
                               last)

        access-token (or (get-in cookies [:headers "authorization"])
                         (get-in cookies [:jwt-token])
                         token-from-headers)]
    access-token))

(defn get-token-claims
  [headers cookies]
  (let [access-token (jwt-access-token headers cookies)]
    (:claims
     (when (seq access-token)
       (try (jwt/str->jwt access-token)
            (catch java.lang.Exception  e (do "prn invalid token" nil))
            (catch java.io.EOFException e (do "prn invalid token" nil)))))))

(defn get-token-string
  [headers cookies]
  (let [access-token (jwt-access-token headers cookies)]
    (when (seq access-token)
      access-token)))

(defn get-token
  [headers cookies]
  (let [access-token (jwt-access-token headers cookies)]
    (when (seq access-token)
      (try (jwt/str->jwt access-token)
           (catch java.io.EOFException e (do "prn invalid token" nil))))))

(comment

  (jwt/str->jwt "a")

  )
