(ns zframes.zquery-string
  (:require
   [zframes.browser]
   [zframes.window-location :as window-location]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [zframes.browser :as browser]))

#?(:clj
   (rf/reg-event-fx
    ::set-qs
    (fn [_ [_ params]]
      {:uri/set-qs params})))

(rf/reg-fx
 :uri/set-qs
 (fn [params]
   #?(:clj
      (let [url (window-location/gen-query-string (dissoc params :title))]
        (swap! browser/state assoc :qs url)
        ;; we need to mimic browser behaviour here
        (rf/dispatch [::qs-changed (subs url 1)]))
      :cljs
      (let [loc (.. js/window -location)
            url (str (window-location/gen-query-string (dissoc params :title)) (.-hash loc))]     
        ;; not working with empty search params - needs "?" at least (see window-location/gen-query-string)
        (.pushState ;; .replaceState
         js/history
         #js{} (:title params)
         url)     
        (rf/dispatch [::qs-changed (.-search loc)])))))

(defn parse-params [s]
  (if (str/blank? s)
    {}
    (reduce
     (fn [acc pair]
       (let [[k v] (str/split pair #"=" 2)]
         (assoc acc (keyword k) #?(:cljs (js/decodeURIComponent v)))))
     {} (-> (str/replace s #"^\?" "")
            (str/split "&")))))

(rf/reg-event-fx
 ::qs-changed
 (fn [{db :db} [_ raw]]
   (let [new (window-location/parse-querystring raw)
         old (:uri/qs db)
         ev (cond
              (and (empty? old) (not-empty new)) [:uri/query-string :init new]
              (and (not-empty new) (not-empty old) (not= old new)) [:uri/query-string :update new]
              (and (not-empty old) (empty? new)) [:uri/query-string :deinit new])]
     (when ev
       {:db (assoc db :uri/qs new)
        :dispatch ev}))))

(rf/reg-fx
 ::init-qs
 (fn [_]
   #?(:clj
      (do
        (swap! browser/state assoc :qs "")
        (rf/dispatch [::qs-changed ""]))
      :cljs
      (let [f #(rf/dispatch [::qs-changed (.. js/window -location -search)])]
        (aset js/window "onpopstate" f)
        (f)))))

(rf/reg-event-fx
 ::init-qs
 (fn [& _]
   {::init-qs {}}))
