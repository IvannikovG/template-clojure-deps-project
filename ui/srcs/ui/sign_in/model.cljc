(ns ui.sign-in.model
  (:require [pager.core :as pager :include-macros true]
            [ui.zenform.model :as zenform]
            [ui.utils :as u]
            [re-frame.core :as rf]
            [ui.routes :as routes]
            [clojure.string :as str]))

(def *ns [::model])
(def page-name :sign-in/index)
(def form-path [page-name :form])
(def fp form-path)
(def index-event :auth/sign-in)

(pager/reg-page page-name)

(def schema
  {:type "form"
   :actions {:submit {:event ::submit :validate? true}}
   :fields
   {:username {:path [:username]
               :validators {:v/required {:event :submit}}
               :widget {:placeholder "Username"}}
    :password {:path [:password]
               :widget {:type :password :placeholder "Password"}
               :validators {:v/required {:event :submit}}}}})

(rf/reg-event-fx
 page-name
 [(rf/inject-cofx :cookie/get :jwt-token)]
 (fn [{db :db cookie :cookie} [_ phase & args]]
   (cond
     (= phase :init)
     (if (:jwt-token cookie)
       {:zframes.redirect/redirect {:uri "/"}}
       {:dispatch [:zenform/init form-path schema]
        :hotkey/add {:submit {:event :zenform/action
                              :event-args [form-path :submit]
                              :key "Enter"}}})
     (= phase :deinit)
     {:db (dissoc db page-name)
      :hotkey/clear [:submit]})))

(rf/reg-event-fx
 ::submit
 (fn [{db :db} [_ form-data]]
   (when-not (get-in db (conj [page-name] :loading))
     (let [body (into {} (map (fn [[k v]] [k (u/b64-encode v)]) form-data))]
       {:json/fetch
        {:uri "/login"
         :method :post
         :is-fetching-path (conj [page-name] :loading)
         :body body
         :success {:event ::success}
         :error {:event :global/error}}
        :db (update-in db [page-name] dissoc :errors)}))))

(rf/reg-event-fx
 ::success
 (fn [{db :db} [_ {{jwt-token :jwt-token} :data :as resp}]]
   (let [r (or (get-in db [:fragment-params :params :back-uri])
               "/how-to-use")
         parsed-jwt (u/parse-id-token jwt-token)]
     {:cookie/set {:jwt-token jwt-token}
      :dispatch-n []
      :db  (-> db
               (assoc-in [:auth :user] parsed-jwt)
               (assoc-in [:cookie :jwt-token] jwt-token))
      :zframes.redirect/redirect {:uri r}})))

(rf/reg-event-db
 ::error
 (fn [db [_ {r :data}]]
   (assoc-in db (conj [page-name] :errors) r)))

(pager/reg-sub
 (fn [page _]
   {}))
