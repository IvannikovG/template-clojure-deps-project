(ns ui.db 
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]))

(rf/reg-sub-raw :route (fn [db _] (reaction (:route @db))))
(rf/reg-sub-raw :db (fn [db] db))

(defn insert-by-path [m [k & ks :as path] value]
  (if ks
    (if (int? k)
      (assoc (or m []) k (insert-by-path (get m k) ks value))
      (assoc (or m {}) k (insert-by-path (get m k) ks value)))
    (if (int? k)
      (assoc (or m []) k value)
      (assoc (or m {}) k value))))

(rf/reg-event-db
 :db/write
 (fn [db [_ path val]]
   (insert-by-path db path val)))

(rf/reg-sub
 :db/read
 (fn [db [_ path]]
   (get-in db path)))
