(ns routing
  (:require [clojure.string :as str]
            [db :as db]
            [clojure.walk :as walk]
            [utils :as u]
            [handler-helpers :as hh]
            [clj-jwt.core  :as jwt]
            [clojure.pprint :as p]))

(def jwt-secret (System/getenv "JWT_SECRET"))

(defn validate-subscription
  [ctx {:keys [valid-until] :as subscr}]
  (when (seq valid-until)
    (let [today (u/today-exact)
          until (u/from-str valid-until)
          valid (u/compare-dates until today)]
      valid)))

(defn create-jwt-token
  [_ctx user-record]
  (let [safe-user (select-keys user-record [:name :id :username])]
    (-> safe-user jwt/jwt
        (jwt/sign :HS256 jwt-secret)
        jwt/to-str)))

(defmulti handler (fn [ctx _] (:active-route (u/deatomize ctx))))

(defn parse-uri [uri]
  (let [[bare-uri query-string] (clojure.string/split uri #"\?")
        split-uri (->> (clojure.string/split bare-uri #"/")
                       (remove (fn [el] (contains? #{nil "" " " "?"} el)))
                       (mapv str/lower-case))]
    {:path split-uri
     :query-string query-string}))

(defn path-matcher
  ([path handler-path] (path-matcher path handler-path []))
  ([path handler-path acc]
   (let [elements-match? (= (first path)
                            (first handler-path))
         match-as-id? (and (associative? (first handler-path))
                           (-> (first path) nil? not))
         paths-match? (if-not elements-match?
                        match-as-id?
                        true)]
     (cond (and (seq acc)
                (every? true? acc)
                (empty? (rest handler-path))
                (empty? (rest path)))        true
           (and (or (empty? (rest handler-path))
                    (empty? (rest path)))
                (some false? acc))           false
           :else
           (path-matcher (rest path) (rest handler-path) (conj acc paths-match?))))))

(defn match-path
  [handler uri]
  (let [{:keys [path _query-string]} (parse-uri uri)
        handler-path (:path handler)]
    (path-matcher path handler-path)))

(defn cookie-checker
  "Checks the validity of token from the session resource"
  [ctx {:keys [cookies] :as _request}]
  (let [access-token-path ["access-token" :value]
        access-token (get-in cookies access-token-path)
        {:keys [_id expiration] :as _session}
        (:resource (db/query-first
                    ctx {:dsql
                         {:ql/type :pg/select
                          :select :*
                          :from {:t :session}
                          :where ^:pg/op [:= [:jsonb/#>> :resource [:id]] access-token]
                          :order-by [:pg/sql "resource#>>'{id}' desc"]
                          :limit 1}}))
        valid-token?   (u/valid-token? (str expiration))]
    valid-token?))

(defn jwt-checker
  [_ctx {:keys [cookies headers] :as _request}]
  (let [access-token (hh/get-token-string headers cookies)
        valid-jwt-token? (when access-token
                           (try (jwt/verify (jwt/str->jwt access-token) jwt-secret)
                                (catch Exception e (prn "Could not verify jwt: " access-token))))]
    valid-jwt-token?))

(defn access-checker
  [ctx
   {:keys [cookies headers] :as _request}
   {:keys [subscription-required
           limited-by-subscription] :as _handler-entry}]
  (let [{:keys [_username id]} (hh/get-token-claims headers cookies)
        subs-query   {:h {:select [[:us.resource :us]]
                          :from [[:UserSubscription :us]]
                          :where [:and
                                  [:= (db/h>> :us.resource [:status]) "active"]
                                  [:= (db/h>> :us.resource [:user-id]) id]]}}
        subscription (when (seq id)
                       (:us (db/query-first ctx subs-query)))
        valid-subscription? (validate-subscription ctx subscription)]
    {:access-checked           valid-subscription?
     :no-sub-needed            (and (not subscription-required)
                                    (not limited-by-subscription))
     :sub-limitation           (seq limited-by-subscription)
     :needed-sub-but-not-found (and subscription-required
                                    (not valid-subscription?))
     :subscription subscription}))

(defn add-access-info
  [request subscription ]
  (assoc request :user-subscription subscription))

(defn routing-mapper
  "By function keyword dispatches to the handler."
  [ctx request]
  (let [api            (get (u/deatomize ctx) :api)
        {:keys [uri request-method]} (select-keys request [:uri :request-method])
        by-method      (fn [[_ h]]
                         (= (str/lower-case (name request-method)) (:method h)))
        by-path        (fn [[_ h]]
                         (let [paths-match? (match-path h uri)]
                           paths-match?))
        by-resource    (fn [[_ h]]
                         (and (:resource h)
                              (str/starts-with? (str uri) "/resource")))

        by-static      (fn [[_ h]]
                         (and (:static h)
                              (contains?
                               (set (str/split (str uri) #"/" ))
                               "static")))

        api-entry      (cond
                         (seq (->> api (filter by-static))) (->> api (filter by-static))
                         (seq (->> api (filter by-resource))) (->> api (filter by-resource))
                         :else (->> api (filter by-method) (filter by-path) (into {})))

        {:keys [login-required? limited-by-subscription] :as handler-entry}  (->> api-entry vals first)
        handler-name   (->> api-entry keys first)
        ctx*           (assoc ctx :active-route handler-name)
        wrap-params    (fn [req] (assoc req
                                        :params (walk/keywordize-keys
                                                 (:params req))
                                        :query-params (walk/keywordize-keys
                                                       (:query-params req))))
        jwt-checked    (jwt-checker ctx* request)
        {:keys [access-checked subscription no-sub-needed
                needed-sub-but-not-found sub-limitation]}
        (access-checker ctx request handler-entry)
        request* (-> request
                     (assoc :subscription subscription))]
    (if (= uri "/")
      (handler (assoc ctx* :active-route :index)
               (-> request* wrap-params))
      (if login-required?
        (if jwt-checked
          (cond
            (and jwt-checked no-sub-needed)
            (handler ctx* (-> request* wrap-params))

            (and jwt-checked access-checked)
            (handler ctx* (-> request*
                              wrap-params
                              (add-access-info subscription)))

            (and jwt-checked sub-limitation)
            (handler ctx* (assoc request* :limited-by-subscription limited-by-subscription))

            (and jwt-checked needed-sub-but-not-found)
            (let [ctx** (assoc-in ctx* [:active-route] :user-handlers.handlers/buy-subscription)]
              (handler ctx** (-> request*
                                 wrap-params)))

            jwt-checked (handler ctx* (wrap-params request*)))
          {:status 403
           :body {:message "Unauthorized access"}})

        (handler ctx* (wrap-params request*))))))


(comment
  (def ctx core/app-context)

  (routing-mapper ctx {:request-method "get" :uri "/cars/123"})

  (routing-mapper ctx {:request-method "get" :uri "/$chinese-record/123"})

  )
