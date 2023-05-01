(ns ui.layout
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [garden.units :as u]
   [garden.selectors :as s]
   [garden.stylesheet :as garden-stylesheet]
   [reagent.core :as reagent]
   [ui.styles :as styles]
   [re-frame.core :as rf]
   [ui.routes :refer [href]]
   [ui.navigation.core :as navigation]
   [clojure.string :as str]))

(def outline "#4D90FE solid 1px")

(def common-style
  [:body
   styles/some-widgets-styles
   styles/basic-style
   styles/crud-styles
   navigation/quick-nav-style

   [:.pull-right {:float "right"}]

   {:font-family "Roboto"
    :font-size "14px"
    :line-height "24px"}

   [:*:focus {:outline :none}]

   [(s/div (s/attr= :tabindex "0") (s/focus)) {:outline outline}]

   [:input:focus {:outline outline}]

   [:.empty {:text-align "center"
             :padding "5px 0"
             :color "#888"}]

   [:h3 {;; :border-bottom "2px solid rgba(53, 59, 80, 0.1)"
         :padding-bottom "19px"
         :font-size "24px"
         :letter-spacing "0.7px"
         :font-weight 500
         :line-height "40px"}]

   [:.search-icon {:position "relative" :right "-20px" :z-index 0 :color "#ddd"}]

   [:.search {:border-top "none"
              :font-weight 300
              :border-right "none"
              :padding-left "24px"
              :position "relative"
              :bottom "-1px"
              :margin-left "20px"
              :width "20em"
              :border-bottom "2px solid #888"
              :border-left "none"}
    [:&:focus {:border-top "none!important"
               :border-right "none!important"
               :border-bottom-color "#0275d8!important"
               :outline "none"
               :border-left "none!important"}]]

   [:.block {:margin-top "20px" :margin-bottom "20px"
             :border-left "2px solid #8bc34a"
             :padding-left "30px"}
    [:&.form {:border-left-color "#ddd" :background-color "#f6f6f6"}]
    [:b {:color "gray"
         :font-weight 300
         :letter-spacing "0.4px"}]]

   [:.assignee {:margin-right "10px"
                :font-size "14px"
                :color "#444"
                :border "1px solid #f1f1f1"
                :display "inline-block;"
                :margin-left "10px"
                :height "26px"
                :border-radius "12px"
                :padding  "1px 10px 1px 1px"}
    [:.fa {:margin "0 5px" :border-radius "50%" :border "1px solid #ddd"}]
    [:img.avatar {:width "19px"
                  :height "19px"
                  :margin "0 5px 0 2px"
                  :border-radius "50%"}]]

   [:.action {:border "1px solid rgba(27, 149, 224, 0.11)"
              :color "#1b95e0!important"
              :cursor :pointer
              :padding "2px 10px"
              :font-size "14px"
              :font-weight 300
              :border-radius "10px" :margin-right "10px"}]
   ])

(defn avatar []
  [:img.avatar
   {:src (str "https://randomuser.me/api/portraits/men/" (rand-int 100) ".jpg")}])

(def layout-styles
  [:div.wrap {:display "flex"
              :flex-direction "column"}

   (garden-stylesheet/at-media
    {:print true}
    [:.content {:padding-top "0px !important"
                ;; :page-break-after :always
                }]
    [:.centered-content {:padding-bottom "0px !important"}]
    [:.nav-cnt {:display :none}])

   [:.top-menu {:position "absolute" :right "40px" :top "10px"}]
   [:.content-wrap {:flex 1}]
   [:.content {:padding-left "0px"
               :margin-right "20px"
               :margin-left "20px"
               :padding-top "32px"}]
   [:.centered-content {:width "100%"
                        :margin-top "26px"
                        :padding-bottom "50px"
                        :margin "0 auto"}]
   [:div.header {:margin-left "250px"
                 :margin-top "65px"}
    [:h1 {:font-size "32px"
          :font-weight :normal
          :line-height "38px"}]]])

(rf/reg-event-db
 :off-navigation
 (fn [db] (assoc db :navigation false :navigation/search "")))

(rf/reg-event-db
 :toggle-navigation
 (fn [db] (-> db
              (update :navigation not)
              (assoc :navigation/search ""))))

#_(rf/reg-sub
 :navigation
 (fn [db] (:navigation db)))

(defn layout [params content]
  (let []
    (fn [params content]
      [:div.wrap
       (styles/style common-style)
       (styles/style layout-styles)
       [navigation/quick-nav]
       [:div.content-wrap
        [:div.content content]]])))
