(ns ui.routes
  (:require
   [re-frame.db :as db]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [zframes.window-location :as window-location]
   [route-map.core :as route-map]))

(def routes
  {:.                    :sign-in/index
   "sign-in"            {:. :sign-in/index}
   "register"           {:. :register/index}})

(defn to-query-params [params]
  (->> params
       (map (fn [[k v]] (str (name k) "=" v)))
       (str/join "&")))

(defn href [& parts]
  (let [params (if (map? (last parts)) (last parts) nil)
        parts (if params (butlast parts) parts)
        url (str "/" (str/join "/" (map (fn [x] (if (keyword? x) (name x) (str x))) parts)))]
    (when-not  (route-map/match [:. url] routes)
      (println (str url " does not match any route")))
    (str "#" url (when params (window-location/gen-query-string params)))))

(defn back [fallback]
  (if-let [item (second (:route/history @db/app-db))]
    {:href (:uri item)}
    {:href fallback}))
