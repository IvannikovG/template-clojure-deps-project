(ns zframes.routing
  (:require
   [clojure.string :as str]
   [clojure.set :as set]

   [re-frame.core :as rf]
   [route-map.core :as route-map]

   [zframes.window-location :as window-location]
   [zframes.browser :as browser]))

(defn dispatch-routes
  ([] (dispatch-routes nil))
  ([_]
   (let [hash
         #?(:cljs (.. js/window -location -hash)
            :clj (:uri @browser/state))]
     #?(:cljs 
        (.scrollTo js/window 0 0))
     (rf/dispatch [:fragment-changed hash]))))

(defn parse-fragment [fragment]
  (let [[path params-str] (-> fragment
                              (str/replace #"^#" "")
                              (str/split #"\?"))]
    {:path path
     :query-string params-str
     :params (window-location/parse-querystring params-str)}))

(defn diff [old new]
  (let [all-keys   (keys (merge old new))
        difference (mapv (fn [key]
                           (let [old-v (get old key)
                                 new-v (get new key)]
                             (cond
                               (= old-v new-v)   {:key key :status :unchanged :old-value old-v}
                               (and old-v new-v) {:key key :status :updated :old-value old-v :new-value new-v}
                               old-v             {:key key :status :deleted :old-value old-v}
                               new-v             {:key key :status :created :new-value new-v}
                               :else             nil))) all-keys)]
    difference))

(rf/reg-event-fx
 :fragment-changed
 (fn [{db :db} [k fragment]]
   (let [{path :path q-params :params qs :query-string} (parse-fragment fragment)]
     (if-let [route (route-map/match [:. path] (:route-map/routes db))]
       (let [params (->> (get-in route [:match :params])
                         (map (fn [[k {:keys [f path value]}]]
                                [k (cond
                                     path (get-in db path)
                                     f (f)
                                     value value)]))
                         (filter second)
                         (remove (fn [[k v]] (contains? q-params k)))
                         (into {}))]
         (if (and (not-empty params)
                  (not= (get-in route [:match :page])
                        (get-in db [:route-map/current-route :match])))
           {:zframes.redirect/redirect {:uri path :params (merge q-params params)}}
           (let [params (assoc (:params route) :params q-params)
                 interceptors (->> (:parents route)
                                   (mapcat :interceptors)
                                   (remove nil?))
                 current-page (or (get-in route [:match :page])
                                  (:match route))
                 route {:match current-page
                        :params params
                        :parents (:parents route)
                        :href fragment
                        :interceptors interceptors}
                 contexts (reduce (fn [acc {c-params :params ctx :context route :.}]
                                    (if ctx
                                      (assoc acc ctx (assoc c-params :. route))
                                      acc)) {} (:parents route))
                 old-page     (get-in db [:route-map/current-route :match])
                 old-params   (get-in db [:route-map/current-route :params])
                 params-diff  (diff (:params old-params) (:params params))

                 page-ctx-events (cond
                                   (= current-page old-page)
                                   (cond (= old-params params) []

                                         (= (dissoc old-params :params)
                                            (dissoc params :params))
                                         [[current-page :params (assoc params :diff params-diff) old-params]]

                                         :else
                                         [[current-page :deinit old-params] [current-page :init params]])
                                   :else
                                   (cond-> []
                                     old-page (conj [old-page :deinit old-params])
                                     true (conj [current-page :init params])))]
             (cond->
                 {:db (assoc db
                             :route/history
                             (conj (take 4 (:route/history db))
                                   {:route current-page :uri fragment})
                             :fragment fragment
                             :fragment-params params
                             :fragment-path path
                             :fragment-query-string qs
                             :route/context contexts
                             :route-map/current-route route)
                  :dispatch-n page-ctx-events}
               (nil? (:pt-id params)) (assoc :effects/set-title path)))))
       {:db (assoc db
                   :fragment fragment
                   :route-map/current-route nil
                   :route-map/error :not-found)}))))

(rf/reg-event-fx
 :route-map/init
 (fn [{:keys [db]} [_ routes]]
   {:db (assoc db :route-map/routes routes)
    :history {}}))

(rf/reg-fx
 :history
 (fn [_]
   #?(:cljs
      (aset js/window "onhashchange" dispatch-routes))
   (dispatch-routes)))

(rf/reg-sub
 :route-map/current-route
 (fn [db _] (:route-map/current-route db)))

(rf/reg-sub
 :route-map/error
 (fn [db _] (:route-map/error db)))
