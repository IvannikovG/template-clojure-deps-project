(ns ui.register.model
  (:require
   [pager.core :as pager :include-macros true]
   [re-frame.core :as rf]
   [ui.utils :as u]
   [ui.styles :as styles]
   [ui.widgets :as wgt]
   [ui.zenform.core :as zf]
   [ui.pages :as pages]))


(def page-name :register/index)
(def form-path [page-name :form])

(pager/reg-page page-name)


(def schema
  {:type "form"
   :actions {:submit {:event ::submit :validate? true}}
   :fields
   {:username {:path [:username]
               :validators {:v/required {:event :submit}}
               :widget {:placeholder "Username"}}
    :email    {:path [:email]
               :widget {:placeholder "Email"}
               :validators {:v/required {:event :submit}
                            :v/email    {:event :submit}}}
    :password {:path [:password]
               :widget {:type :password :placeholder "Password"}
               :validators {:v/required {:event :submit}}}}})


(pager/reg-init
 ^{:not-ready [:init]}
 (fn [{db :db} _ prms]
   {:dispatch [:zenform/init form-path schema]}))

(rf/reg-event-fx
 ::submit
 (fn [{db :db} [_ form-data]]
   (when-not (get-in db (conj [page-name] :loading))
     {:json/fetch
      {:uri "/register"
       :method :post
       :is-fetching-path (conj [page-name] :loading)
       :body (into {} (map (fn [[k v]] [k (u/b64-encode v)]) form-data))
       :success {:event ::success}
       :error {:event ::error}}
      :db (update-in db [page-name] dissoc :errors)})))

(rf/reg-event-fx
 ::success
 (fn [{db :db} [_ data]]
   {:zframes.redirect/redirect {:uri "/sign-in"}}))

(rf/reg-event-db
 ::error
 (fn [db [_ {r :data}]]
   (assoc-in db (conj [page-name] :errors) r)))

(pager/reg-sub
 (fn [page _]
   {}))
