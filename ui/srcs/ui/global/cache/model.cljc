(ns ui.global.cache.model
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::set-clr-check-number
 (fn [db [_ check-number]]
   (assoc-in db [:global/cache :claimresponse :check-number] check-number)))

(rf/reg-sub
 ::clr-check-number
 (fn [db _]
   (get-in db [:global/cache :claimresponse :check-number])))

(rf/reg-event-fx
 ::loading-error
 (fn [_ [_ {msg :msg}]]
   {:alerts/fail {:text msg :duration 10}}))

(rf/reg-event-db
 ::locations-loaded
 (fn [db [_ {data :data}]]
   (assoc-in db [:global/cache :locations] (map :resource (:entry data)))))

(rf/reg-sub
 ::locations
 (fn [db _]
   (get-in db [:global/cache :locations])))

(rf/reg-event-fx
 ::fetch-providers
 (fn [{db :db} _]
   {:json/fetch
    {:uri "/Practitioner"
     :params {:.active "true"}
     :success {:event ::providers-loaded}
     :error  {:event ::loading-error
              :msg "Providers loading error"}}}))

(rf/reg-event-db
 ::providers-loaded
 (fn [db [_ {data :data}]]
   (assoc-in db
             [:global/cache :providers]
             (->> data
                  :entry
                  (map :resource)
                  (sort-by (fn [p] (get-in p [:name 0 :family])))))))

(rf/reg-sub
 ::providers
 (fn [db _]
   (get-in db [:global/cache :providers])))
