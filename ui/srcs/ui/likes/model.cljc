(ns ui.likes.model
  (:require [pager.core :as pager :include-macros true]
            [ui.zenform.model :as zenform]
            [ui.utils :as u]
            [re-frame.core :as rf]
            [ui.routes :as routes]
            [clojure.string :as str]))


(def page-name :likes/index)

(pager/reg-page page-name)

(pager/reg-init
 ^{:not-ready [:init]}
 (fn [{db :db} _ _]
   (let [author (get-in db [:auth :user :username])]
     {:json/fetch {:uri ["$get-user-likes"]
                   :params {"author" author}
                   :success {:event ::loaded}}})))

(pager/reg-event-fx
 ::load-like
 (fn [{db :db} _]
   {:json/fetch {:uri ["$get-user-likes"]
                 :success {:event ::loaded}}} ))

(pager/reg-event-fx
 ::delete-like
 (fn [{db :db} like]
   (let [like-to-remove (:id like)]
     {:json/fetch {:uri ["$delete-like"]
                   :method  "post"
                   :body    {:like-id like-to-remove}
                   :success {:event ::load-like}}})))

(pager/reg-event-db
 ::loaded
 ^{:ready [:init]}
 (fn [_page {:keys [data]}]
   (prn "Data::  " (:records data))
   {:likes (:records data)}))

(pager/reg-sub
 (fn [page _]
   {}))
