(ns ui.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [clojure.string :as str]
   [cljsjs.react]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ui.config]

   [zframes.effects]
   [zframes.routing]
   [zframes.zquery-string :as qs]
   [zframes.xhr]
   [zframes.debounce]
   [zframes.cookies :as cookies]
   [zframes.openid :as openid]
   [zframes.redirect :as redirect]
   [zframes.hotkeys :as hotkeys]
   [zframes.window-location :as location]

   [ui.widgets.alerts]

   [ui.search]

   [ui.events]
   [ui.db]
   [ui.pages :as pages]
   [ui.utils :as utils]
   [ui.routes :as routes]
   [ui.layout :as layout]
   [ui.global.cache.model :as cache]

   [ui.register.core :as register]
   [ui.sign-in.core :as sign-in]
   [ui.navigation.core]))


(defn current-page []
  (let [{page :match params :params href :href} @(rf/subscribe [:route-map/current-route])
        cmp (get @pages/pages page)
        route-error @(rf/subscribe [:route-map/error])
        params (assoc params
                      :href href
                      :context {}
                      :route page
                      :route-ns (when page (namespace page)))]
    (cond
      (= page :sign-in/index) [sign-in/sign-in params]
      (= page :register/index) [register/register params]

      (and page cmp) [layout/layout params [cmp params]]
      (and page (not cmp)) [:div.not-found (str "Component not found: " page)]
      (= route-error :not-found) [:div.not-found (str "Route not found")])))

(rf/reg-event-fx
 :user/profile
 (fn [{db :db} [_ phase]]
   {:json/fetch
    {:uri "/User"
     :params {:_id (get-in db [:auth :user :sub])}
     :success {:event ::user-loaded}
     :error   {:event :global/error
               :desc  "User fetching  error"}}}))

(defn ->user [resp]
  (-> resp :entry first :resource (dissoc :meta :password)))

(rf/reg-sub
 ::user-display
 (fn [db _]
   (str (get db :user))))

(rf/reg-event-db
 ::user-loaded
 (fn [db [_ {resp :data}]]
   (assoc db :user (->user resp))))

(rf/reg-event-fx
 :auth/signout
 (fn [{db :db} _]
   {:cookie/remove {:key :jwt-token}
    :zframes.redirect/redirect {:uri "/sign-in"}}))


(rf/reg-event-fx
 ::initialize
 [(rf/inject-cofx :cookie/get :jwt-token)
  (rf/inject-cofx :window-location)]
 (fn [{{jwt :jwt-token} :cookie
       {qs :hash-qs hash :hash host :hostname} :location
       :as cofx} _]
   (let [cfg {:base-url
              (case host
                "localhost"              "http://localhost:7002"
                "themultidictionary"     "https://themultidictionary.com"
                (str (.. js/window -location -protocol) "//"
                     (.. js/window -location -hostname) ":443"))}
         db (merge (:db cofx) {:config cfg})
         redir
         (cond-> {:uri "/sign-in"}
           (and (not= hash "/sign-in")
                (not= hash "/register")
                (not (empty? hash))
                (not (str/includes? hash "back-uri")))
           (assoc :params {:back-uri hash}))]
     {:hotkey/init {"Alt+Enter" {:event :toggle-navigation}}
      :dispatch-n (cond-> [[:route-map/init routes/routes]
                           [::qs/init-qs]]
                    #_#_(not-empty jwt) (conj [:user/profile]))
      :db (if jwt
            (let [parsed-jwt (utils/parse-id-token jwt)]
              (-> db
                  (assoc-in [:auth :user]  parsed-jwt)))
            db)
      ::redirect/redirect (when (empty? jwt) redir)})))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render
   [current-page]
   (.getElementById js/document "app")))

(defn ^:export main []
  (rf/dispatch-sync [::initialize])
  (mount-root))
