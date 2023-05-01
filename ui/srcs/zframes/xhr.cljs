(ns zframes.xhr
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [cljs.reader :as reader]
   [goog.net.cookies]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [re-frame.db :as db]))

(defn sub-query-by-spaces
  [k s] (->> (str/split s #"\s+")
             (mapv (fn [v] (str (name k) "=" v)))
             (str/join "&")))

(defn to-query [params]
  (->> params
       (mapcat (fn [[k v]]
                 (cond
                   (vector? v) (mapv (fn [vv] (str (name k) "=" vv)) v)
                   (set? v) [(str (name k) "=" (str/join "," v))]
                   :else [(str (name k) "=" v)])))
       (str/join "&")))

(defn make-form-data [files]
  (let [form-data (js/FormData.)]
    (doall
     (for [[i file] (map-indexed vector files)]
       (let [;; .-name for real files - upload Claim responses
             ;; (:name file) for webcam images - on Coverage form
             [file-object name] (if (map? file)
                                  [(:file file) (or (:file-name file) (:name file))]
                                  [file (.-name file)])
             file-name (or name (str "file" i))]
         (.append form-data file-name file-object file-name))))
    form-data))

(defn *json-fetch [{:keys [uri headers is-fetching-path params success error fhir-destination fetched-path]
                    :as opts}
                   abort-signal]
  (let [jwt (reader/read-string (or (.get goog.net.cookies "jwt-token") "nil"))
        current-page (get-in @db/app-db [:route-map/current-route :href])
        base (get-in @db/app-db [:config :base-url])
        url (cond
              (vector? uri) (str base "/" (str/join "/" uri))
              (str/starts-with? uri "http") uri 
              :else (str base uri))]
    (let [headers (cond-> {"Accept" "application/json"}
                      jwt (assoc "Authorization" (str "Bearer " jwt))
                      (nil? (:files opts)) (assoc "Content-Type" "application/json")
                      true (merge (or headers {})))

            fetch-opts (-> (merge {:method "get" :mode "cors"} opts)
                           (dissoc :uri :headers :success :error :params :files)
                           (assoc :headers headers))

            fetch-opts (cond-> fetch-opts
                         (:body opts) (assoc :body (.stringify js/JSON (clj->js (:body opts))))
                         (:body-raw opts) (assoc :body (:body-raw opts))
                         (:files opts) (assoc :body (make-form-data (:files opts))))

            uniq-req-name (gensym (str uri ":"))

            error-ev
            (fn [data]
              (let [ev-body {:data data :request opts}]
                (if-let [event (:event error)]
                  [event (merge error ev-body)]
                  [:global/error ev-body])))

            success-ev
            (fn [data] [(:event success) (merge success {:request opts :data data})])]

        (rf/dispatch [::fetch-start uniq-req-name is-fetching-path])

        (-> (js/fetch (str url (when params (str "?" (to-query params))))
                      (clj->js (cond-> fetch-opts
                                 abort-signal (assoc :signal abort-signal))))
            (.then
             (fn [resp]
               (.then (.json resp)
                (fn [doc]
                  ;; json parsed
                  (let [body (js->clj doc :keywordize-keys true)]
                    (when fetched-path (rf/dispatch [::fetched fetched-path]))
                    (rf/dispatch [::fetch-end uniq-req-name is-fetching-path])
                    (if (.-ok resp)
                      (do
                        (when fhir-destination (rf/dispatch [::fhir-destination fhir-destination body]))
                        (when success (rf/dispatch (success-ev body))))
                      (rf/dispatch (error-ev body)))))
                (fn [_]
                  ;; no json
                  (rf/dispatch [::fetch-end uniq-req-name is-fetching-path])
                  (if (.-ok resp)
                    (rf/dispatch (success-ev {}))
                    (rf/dispatch (error-ev {}))))))
             (fn [err]
               ;; network errors go here
               (if (= (.-name err) "AbortError")
                 (prn (str "Request to '" (str url (when params (str "?" (to-query params)))) "' aborted"))
                 (do
                   (rf/dispatch [::fetch-end uniq-req-name is-fetching-path])
                   (rf/dispatch (error-ev {:message "Internal error"}))))))))
    #_(if (or jwt (str/includes? url "/$auth"))

      ;; jwt = nil
      (rf/dispatch [:zframes.redirect/redirect
                    (cond-> {:uri "/sign-in"}
                      (not= current-page "#/sign-in") (assoc :params {:back-uri current-page}))]))))

(defonce abort-controller (atom (js/AbortController.)))

(defn json-fetch [opts]
  (let [page-init-req? (some :page-init-request? (flatten [opts]))
        [prev-req-abort-controller req-abort-signal]
        (when page-init-req?
          [@abort-controller (.-signal (reset! abort-controller (js/AbortController.)))])]
    (when prev-req-abort-controller
      (.abort prev-req-abort-controller))
    (if (or (vector? opts) (list? opts))
      (doseq [o opts] (*json-fetch o req-abort-signal))
      (*json-fetch opts req-abort-signal))))

(rf/reg-fx :json/fetch json-fetch)
(rf/reg-fx ::json-fetch json-fetch)

(rf/reg-event-db
  ::fhir-destination
  (fn [db [_ path data]]
    (if (= "Bundle" (:resourceType data))
      (assoc-in db path (map :resource (:entry data)))
      (assoc-in db path data))))

(rf/reg-event-db
  ::fetched
  (fn [db [_ path]]
    (assoc-in db path true)))

(rf/reg-event-db
 ::fetch-start
 (fn [db [_ uniq-req-name path]]
   (if path
     (if (vector? path)
       (assoc-in db path true)
       (assoc db path true))
     db)))

(rf/reg-event-db
 ::fetch-end
 (fn [db [_ uniq-req-name path]]
   (if path
     (if (vector? path)
       (assoc-in db path false)
       (assoc db path false))
     db)))

