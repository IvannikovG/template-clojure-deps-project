(ns handlers
  (:require [db :as db]
            [routing :refer [handler] :as r]
            [clojure.string :as str]
            #_[utils :as u]
            [ring.util.response :as rur]
            #_#_[next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [clj-jwt.core  :as jwt]
            [zen-connection :as zc]
            #_[zen.v2-validation :as zv]
            [clj-time.core :as t]
            [handler-helpers :as hh]
            [clojure.pprint :as pprint]
            [ring.middleware.cookies :refer [cookies-response]]
            [dsql.pg :as d]
            [rpc-methods :as rpcm]
            [zen.core :as zen]
            [clojure.walk :as walk]
            [clojure.java.io :as io]))

(def global-page-size 50)

(defn default-handler [ctx request]
  (let [params         (:params request)
        form-params    (:form-params request)
        query-params   (:query-params request)
        query-string   (:query-string request)
        request-method (:request-method request)
        uri            (:uri request)
        body           (:body request)
        headers        (:headers request)
        cookies        (:cookies request)
        seen-path      ["seen" :value]
        seen?          (get-in cookies seen-path)
        seen-tim-path  ["time" :value]
        seen-time      (Integer/parseInt (get-in cookies seen-tim-path "0"))]
    (let [#_#_visit-count (get-in cookies ["counter" :value] 0)]
      #_(println visit-count)
      #_(println "Cookies" cookies)
      #_(println "KEYS" (keys cookies))
      (cookies-response
       {:status 404
        #_#_:headers {"content-type" "application/json"}
        #_#_:cookies (-> cookies
                     (assoc-in seen-path true)
                     (assoc-in seen-tim-path (inc seen-time)))
        :body  {:uri uri
                :request {#_#_:headers        headers
                          #_#_:ctx            (str (dissoc (u/deatomize ctx) :ztx))
                          :params         params
                          #_#_:body           (if (associative? body) body (str body))
                          :query-params   query-params
                          :query-string   query-string
                          :request-method request-method}}}))))

#_(defmulti generate-rpc-error (fn []))

(defn validate-rpc
  [ztx rpc]
  (let [is-request?             (fn [el] (-> el :id))
        _is-notification?       (fn [el] (not (is-request? el)))
        method-name             (-> rpc :method symbol)
        method-schema           (->> method-name (zen/get-symbol ztx))
        validation-result       (cond (vector? rpc)
                                      {:resource rpc
                                       :validation {:message "validation not implemented"} #_(zen/validate-schema ztx rpc-batch-schema rpc)}
                                      :else
                                      {:resource rpc
                                       :validation (zen/validate-schema ztx (zen/get-symbol ztx method-name) rpc)})]
    validation-result))

(comment
  (def p {:method "rpc/test-rpc", :jsonrpc "2.0", :params {:a 1, :b 2}, :id 1})

  (def z (zen/new-context))

  (:errors (zen/read-ns z 'rpc))

  (zen/validate-schema ztx (zen/get-symbol ztx 'rpc/test-rpc)
                {:method "rpc/test-rpc", :jsonrpc "2.0"
                 :params {:b 2}, :id 1})

  (zen/validate ztx #{'rpc/test-rpc2}
                       {:method "rpc/test-rpc2", :jsonrpc "2.0"
                        :a 2
                        :params {:b 2}, :id 1})

  )

(defn create-rpc-response
  [_ctx validation {:keys [id jsonrpc method params] :as _resource} response]
  (let [contains-errors? (-> validation :errors seq)
        meta             {:requesting-method method :requesting-params params}
        error-code "1"]
    (if contains-errors?
      {:error {:code error-code
               :data (-> validation :errors seq)
               :message "Contained rpc validation errors"}
       :jsonrpc jsonrpc :id id :meta meta}
      {:meta meta :jsonrpc jsonrpc :id id
       :result response})))

(defn validate-rpc-response
  [ztx rpc-response]
  (let [rpc-response-schema        (zen/get-symbol ztx 'rpc/rpc-response)
        rpc-resp-validation-result (zen/validate-schema ztx rpc-response-schema rpc-response)]
    rpc-resp-validation-result))

(defmethod handler :rpc
  [ctx {:keys [body] :as _request}]
  "Can't use batches of rpc right now"
  (let [ztx                   (:ztx ctx)
        #_#_rpc-type              (if (vector? body) :batch :single)
        available-rpc-methods (conj (zen/get-tag ztx 'rpc/rpc-request-method)
                                    (zen/get-tag ztx 'rpc/rpc-request-notification))
        {:keys [validation resource] :as _validation-result} (validate-rpc ztx body)
        rpc-method-exists?    (contains? available-rpc-methods (-> body :method symbol))]
    (if rpc-method-exists?
      (let [rpc-result            (rpcm/rpc-handler ctx body (-> body :method symbol))
            rpc-result*           (create-rpc-response ctx validation resource rpc-result)
            rpc-res-validation    (->> rpc-result* (validate-rpc-response ztx))
            rpc-result-valid?     (->> rpc-res-validation :errors seq not)]
        (when (not rpc-result-valid?)
          (prn "Called RPC: " (-> body :method symbol)
               " Invalid response: " (->> rpc-result* rpc-res-validation)))
        (cond-> {:body rpc-result*}
          (seq (-> rpc-result* :error)) (assoc :status 400)
          (not (-> rpc-result* :error)) (assoc :status 200)))
        {:status 400
         :body {:jsonrpc "2.0"
                :id      (-> body :id)
                :error {:message "No method found"
                        :data    {:requested-method (-> body :method)}}}})))

(defmethod handler :index
  [_ctx _request]
  (let [static-file-resp (slurp (io/resource "html/index.html"))]
   (if static-file-resp
      (assoc-in {:body static-file-resp} [:headers "Content-Type"] "text/html")
      {:status 404
       :body {:message "Problems with :index endpoint"}
       :headers {"Content-Type" "text/javascript"}})))

(defmethod handler :static
  [_ctx request]
  (let [uri-array        (->> (-> request :uri str
                                  (str/replace #"static" "")
                                  (str/split   #"/"))
                              (drop-while str/blank?)
                              (remove str/blank?))
        audio?           (->> (-> request :uri str
                                  (str/includes? "audio")
                                  #_(str/split   #"/")))

        audio-path       (->> (-> request :uri str
                                 (str/replace #"static" "")
                                 (str/replace #"html" "")
                                 (str/split   #"/"))
                              (remove str/blank?))
        audio-file-path  (->> audio-path (str/join "/"))
        file-path        (->> uri-array (str/join "/"))
        file-type        (-> uri-array last str (str/split #"\.") last)
        get-content-type (fn [el] (get {"js"    "text/javascript"
                                        "html"  "text/html"
                                        "svg"   "image/svg+xml"
                                        "css"   "text/css"
                                        "woff"  "font/woff"
                                        "ttf"   "font/ttf"
                                        "woff2" "font/woff2"
                                        "mp3"   "audio/mp3"}
                                       el "text/html"))
        content-type     (get-content-type file-type)
        release?         (= (System/getenv "RELEASE") "rel")
        file-path*       (if (and release? (= file-type "js"))
                           (str "release/" file-path)
                           file-path)
        static-file-resp (try (if audio?
                                (io/input-stream (io/resource (str audio-file-path)))
                                (slurp
                                 (io/resource (str file-path*))))

                              (catch Exception e
                                {:status 404
                                 :body {:message "No resource found"
                                        :exception (str e)}}))]
    (cond
      (= (:status static-file-resp) 404)
      {:status 404
       :body (:body static-file-resp)}
      static-file-resp (assoc-in {:body static-file-resp}
                                 [:headers "Content-Type"] content-type)

      :else {:status 404
             :body {:message "No js file"}
             :headers {"Content-Type" "text/javascript"}})))


(defmethod handler :default
  [ctx request]
  (default-handler ctx request))


(defmethod handler :cars
  [ctx _request]
  (let [cars (db/query ctx "select * from car limit 10 offset 10")]
    {:status 200
     :body cars}))


(defmulti resource-handler-by-request-method (fn [_ctx request]
                                               (keyword (:request-method request))))

(defmethod resource-handler-by-request-method :get
  [ctx {:keys [uri]}]
  (let [resources {}
        spec {} ;; TODO: this two!
        valid-uri-string?   false ;; TODO:: this!
        parsed-query-string (hh/parse-query-string uri)
        sql-to-run          (hh/build-sql-from-query-config parsed-query-string)
        db-response         (try (db/query ctx sql-to-run)
                                 (catch Exception e
                                   {:errors "Could not get a db response from :Resource handler"
                                    :exception (.getMessage e)
                                    :method "Default resource-handler"
                                    :sql (d/format (:dsql sql-to-run))}))]
    {:status 200
     :body   db-response}))

(defmethod resource-handler-by-request-method :post
  [ctx {:keys [uri body _params]}]
  (let [spec              {} ;; TODO: this two!
        valid-uri-string? false
        response          (db/save-resource! ctx body)]
    {:status 200
     :body  {:uri uri
             :response response}}))

(defmethod resource-handler-by-request-method :delete
  [ctx {:keys [uri body _params]}]
  (let [{:keys [ids resourceType]} body]
    (if (seq resourceType)
      (let [db-response (db/exec! ctx {:dsql {:ql/type :pg/delete
                                              :from (keyword resourceType)
                                              :where [:in
                                                      [:resource#>> [:id]]
                                                      [:pg/sql (format "(%s)" (->> ids
                                                                                  (mapv (fn [el]
                                                                                          (str "'" el "'")))
                                                                                  (str/join #", ")))]]
                                              :returning :id}})]
        {:status 200
         :body {:uri uri
                :response ids
                :db-response db-response}})
      {:status 200
         :body {:response (format "Resource %s does not exist" resourceType)
                :resourceType resourceType
                :ids ids}})))


(defmethod handler :resource
  [ctx request]
  (resource-handler-by-request-method ctx request))


(defmethod handler :jwt-required
  [ctx {:keys [body] :as _request}]
  {:status 200
   :body {:a 1}})

(defmethod handler :test
  [ctx {:keys [cookies query-params headers] :as _request}]
  {:status 200
   :body (db/exec! ctx "select 1;")})
