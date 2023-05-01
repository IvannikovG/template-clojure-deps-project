(ns zframes.redirect
  (:require
   [re-frame.core :as rf]
   [zframes.window-location :as window-location]
   [zframes.routing :as routing]
   [zframes.browser :as browser]
   [clojure.string :as str]))

(defn ->string [uri]
  (->> uri
       (map name)
       (str/join "/")
       (str "#/")))

(defn redirect [opts]
  (when-let [{:keys [uri params]} opts]
    (let [uri* (cond
                 (vector? uri) (->string uri)
                 :else uri)
          url (str uri*
                   (when params
                     (window-location/gen-query-string params)))]
      #?(:cljs
         (set! (.-hash (.-location js/window)) url)
         :clj
         (do
           (swap! browser/state assoc :uri url)
           (routing/dispatch-routes))))))

;; TODO remove second iface
(rf/reg-fx ::redirect redirect)
(rf/reg-fx ::page-redirect redirect)

(rf/reg-fx
  ::redirect-raw
  (fn [url]
    #?(:cljs
       (set! (.-hash (.-location js/window)) url)
       :clj
       (do
         (swap! browser/state assoc :uri url)
         (routing/dispatch-routes)))))

(rf/reg-event-fx
 ::redirect-raw
 (fn [fx [_ url]]
   {::redirect-raw url}))

(rf/reg-event-fx
 ::redirect
 (fn [fx [_ opts]]
   {::redirect opts}))

(rf/reg-event-fx
 ::go-back
 (fn [fx [_ opts]]
   {::redirect (-> fx :db :route/history second)}))

(rf/reg-fx
 ::reload
 (fn []
   #?(:cljs (.reload (.-location js/window))
      :clj (reset! browser/state {:uri "#/"}))))

(rf/reg-event-fx
 ::merge-params
 (fn [{db :db} [_ params]]
   (let [pth (get db :fragment-path)
         nil-keys (reduce (fn [acc [k v]]
                            (if (nil? v) (conj acc k) acc)) [] params)
         old-params (or (get-in db [:fragment-params :params]) {})]
     {::redirect {:uri pth
                  :params (apply dissoc (merge old-params params) nil-keys)}})))

(rf/reg-event-fx
 ::merge-graphemes-params
 (fn [{db :db} [_ params]]
   (let [current-form-graphemes (filter identity (distinct (:current-form-graphemes (:cards/index db))))
         pth (get db :fragment-path)
         nil-keys (reduce (fn [acc [k v]]
                            (if (nil? v) (conj acc k) acc)) [] params)
         old-params (or (get-in db [:fragment-params :params]) {})]
     {:dispatch-n [[::redirect {:uri pth
                                :params (apply dissoc (merge old-params params) nil-keys)}]]})))


(rf/reg-event-fx
 ::set-params
 (fn [{db :db} [_ params]]
   (let [pth (get db :fragment-path)
         nil-keys (reduce (fn [acc [k v]]
                            (if (nil? v) (conj acc k) acc)) [] params)]
     {::redirect {:uri pth
                  :params (apply dissoc params nil-keys)}})))
