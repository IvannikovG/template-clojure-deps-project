(ns ui.navigation.model
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]
   [ui.routes :refer [href]]
   [ui.utils :as utils]))


(rf/reg-event-db
 ::off-navigation
 (fn [db] (assoc db :navigation false :navigation/search "")))

(rf/reg-sub
 ::nav-active
 (fn [db] (-> db :navigation)))

(rf/reg-event-db
 ::toggle-navigation
 (fn [db] (-> db
              (update :navigation not)
              (assoc :navigation/search ""))))

(rf/reg-event-fx
 :navigation/index
 (fn [_] {}))

(def default-nav
  [{:title "User"
    :items [{:href (href "cards") :icon :calendar :text "Hieroglyphs"}
            {:href (href "graphemes" ) :icon :user-md :text "Graphemes"}
            {:href (href "groups") :icon :calendar :text "Groups"}
            {:href (href "total-knowledge") :icon :calendar
             :text "Total knowledge"}
            {:href (href "User-created") :icon :calendar :text "Tags"}]}])

(defn filter-nav [sections txt]
  (let [txt (when txt (str/lower-case txt))]
    (if (or (nil? txt) (str/blank? (str/trim txt)))
      sections
      (->> sections
           (mapv
            (fn [sec]
              (update sec
                      :items (fn [xs]
                               (->> xs
                                    (filterv (fn [x] (str/includes? (str/lower-case (:text x)) txt))))))))))))

(rf/reg-sub
 ::navigation
 (fn [db _]
   (let [nav default-nav]
     nav)))

(rf/reg-event-db
 ::search
 (fn [db [_ v]]
   (assoc db :navigation/search v)))

(rf/reg-sub
 ::navigation-search
 (fn [db _]
   {:could-be-hided? (not (contains? #{"" "/"} (:fragment-path db)))
    :routes (:navigation/search db)}))

(rf/reg-sub
 ::navigation
 (fn [db _]
   (let [nav default-nav]
     (filter-nav nav (:navigation/search db)))))

(rf/reg-sub
 ::quick-nav
 (fn [db _]
   (let [current-uri (:fragment-path db)]
     {:qnav
      (->> [[{:uri "/graphemes" :href (href "graphemes") :icon "graphemes" :text "Graphemes grid"}]
            [{:uri "/cards" :href (href "cards") :icon "hieroglyphs" :text "Cards grid"}]
            [{:uri "/groups" :href (href "groups") :icon "groups" :text "Groups"}]
            [{:uri "/likes" :href (href "likes") :icon "groups" :text "Likes"}];; TODO needs like icon
            ;; TODO needs like icon
            [{:uri "/user-tags" :href (href "get-user-tags") :icon "tags" :text "Tags"}]
            [{:uri "/edit-card-view" :href (href "edit-card-view") :icon "groups" :text "Card view"}]
            #_[{:uri "/total-knowledge" :href (href "total-knowledge") :icon "claims" :text "Knowledge"}]
            ]
           (map (partial map #(cond-> % (= current-uri (:uri %)) (assoc :active? true)))))})))
