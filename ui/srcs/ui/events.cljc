(ns ui.events
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [ui.utils :as utils]))

(defmulti paramize (fn [res _] res))

(defmethod paramize :Coverage
  [_ params]
  {".subscriber.id" (:pt-id params)})

(defmethod paramize :PokitdokEligibilityResponse
  [_ params]
  {".coverage.id" (:cov-id params)
   "_sort" "-_lastUpdated"})

(defmethod paramize :Encounter
  [_ params]
  {".subject.id" (:pt-id params)
   "_sort" "-.period.start"
   "_count" 1000}) ;; direct big count for using subject.id index in postgres query plan

(rf/reg-event-fx
 ::load
 (fn [{db :db} [_ resource]]
   {:json/fetch
    {:uri (str (get-in db [:db :config :base-url]) "/" (name resource))
     :token (get-in db [:auth :id_token])
     :fetching-path [resource :loading]
     :params (paramize resource (:fragment-params db))
     :success {:event ::load-done
               :resource resource}}}))

;; todo indexing could be implemented in form of {index-map value}
;; where index-map is a map of 'indexed' fields like {:id ... :patient-id ...}
;; this could help to extract values fast without need to filter through collections
(rf/reg-event-db
 ::load-done
 (fn [db [_ {:keys [resource data]}]]
   (update-in db [:dict resource] merge
              (reduce (fn [acc {r :resource}]
                        (assoc acc (:id r) r)) {} (:entry data)))))

(rf/reg-event-fx
 ::load-resource
 (fn [{db :db} [_ resource {:keys [id]}]]
   {:json/fetch
    {:uri (str (get-in db [:db :config :base-url]) "/" (name resource) "/" id)
     :token (get-in db [:auth :id_token])
     :success {:event ::resource-loaded
               :resource resource}}}))

(rf/reg-event-db
 ::resource-loaded
 (fn [db [_ {:keys [resource data]}]]
   (assoc-in db [:dict resource (:id data)] data)))

(rf/reg-sub
 ::raw-by-id
 (fn [db [_ name id]] (get-in db [:dict name id])))

(defn active-coverages
  [db [_ pt-id]]
  (->>
   (vals (get-in db [:dict :Coverage]))
   (filter (fn [c]
             (and (= (get-in c [:subscriber :id]) pt-id)
                  (not= (:status c) "cancelled"))))
   (sort-by :order)))

(rf/reg-sub ::active-coverages active-coverages)

(defn coverages
  [db [_ pt-id]]
  (->>
   (vals (get-in db [:dict :Coverage]))
   (filter (fn [c]
             (= (get-in c [:subscriber :id]) pt-id)))
   (sort-by :order)))

(rf/reg-sub ::coverages coverages)

(rf/reg-sub
 ::get
 (fn [db [_ path]]
   (get-in db (if (vector? path) path [path]))))

(rf/reg-event-fx
 :global/error
 (fn [{db :db} [_ {desc :desc {msg :message :as d} :data p :path}]]
   (cond-> {:db db}
     msg (assoc :alerts/fail msg)
     (and (nil? msg) desc) (assoc :alerts/fail desc)
     p (assoc-in (into [:db] p) d)
     :else (update-in [:db :errors] conj d))))

(rf/reg-sub
 :global/errors
 (fn [db _] (:errors db)))

(rf/reg-event-fx
 :global/success
 (fn [{{{:keys [match params]} :route-map/current-route} :db} [_ {:keys [message data]}]]
   (let [msg (or message (:message data))]
     (cond-> {:dispatch [match :init params]}
       msg (assoc :alerts/success msg)))))

(rf/reg-event-fx
 :global/reload
 (fn [{{{:keys [match params]} :route-map/current-route} :db} _]
   {:dispatch [match :init params]}))


(rf/reg-event-fx
 :global/open-link
 (fn [_ [_ resource method params]]
   {:json/fetch
    {:uri (str "/$tokenize/" resource "/" method)
     :params params
     :is-fetching-path [:tokenize-link :fetching]
     :success {:event ::tokenized-link-loaded}}}))

(rf/reg-event-fx
 ::tokenized-link-loaded
 (fn [_ [_ {{url :url} :data}]]
   {:effects/open-link url}))

(rf/reg-fx
 :effects/open-link
 (fn [url]
   #?(:cljs
      (.open js/window url "_blank")
      :clj
      (rf/dispatch [:db/assoc :opened-url url]))))

(rf/reg-sub
 :global/open-link-fetching
 (fn [db _] (get-in db [:tokenize-link :fetching])))
