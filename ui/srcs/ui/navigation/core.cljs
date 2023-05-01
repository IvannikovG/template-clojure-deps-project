(ns ui.navigation.core
  (:require
   [re-frame.core :as rf]
   [ui.styles :as styles]
   [ui.pages :as pages]
   [ui.routes :refer [href]]
   [ui.config :as c]
   [ui.utils :as utils]
   [ui.navigation.model :as model]
   [clojure.string :as str]))

(def sidebar-width "calc(40px + 1.2vh)")

(def nav-style
  [:.navigation {:display "flex"
                 :position "fixed"
                 :z-index "200"
                 :height "100%"
                 :align-items :flex-start
                 :top 0
                 :left "0"
                 :right "0px"
                 :color "white"
                 :background-color "rgba(63,69,90,0.7)"}
   [:.title-cat {:border-bottom "1px solid #D8D8D8"
                 :color "#3F455A"
                 :font-size "14px"
                 :margin "2px 5px"
                 :padding-top "15px"
                 :padding-bottom "5px"
                 :font-weight "bold"
                 :margin-top "15px"
                 :margin-bottom "6px"}]

   [:.empty-spacer {:min-width "40px"}]

   [:.menu-block {:min-width "670px"
                  :background-color "#F5F5F5"
                  :border-radius "0 0 8px 0"}]
   [:.search-bar {:height "250px"
                  :display "flex"}
    [:.fas {:color "#353B50"
            :padding-left "10px"}]
    [:.space-for-button {:width sidebar-width
                         :display "flex"
                         :justify-content "center"
                         :align-items "center"
                         :border-bottom "1px solid #D8D8D8"}
     [:&.active {:border-bottom "none"
                 :background-color "#017AFF"
                 :cursor "pointer"}]]
    [:.comment {:text-align "right"
                :padding-right "40px"
                :border-bottom "1px solid #D8D8D8"
                :border-radius "0px 8px 0px 0px"
                :width "45%"
                :padding-top "13px"
                :background-color "white"
                :color "#B8B8B8"
                :font-family "Roboto"
                :font-weight "bold"
                :font-size "12px"}]
    [:.nav-search {:background-color "white"
                   :border-left "none"
                   :width (str "calc(55% - " sidebar-width ")")
                   :border-right "none"
                   :margin-left "0px"
                   :border-top "none"
                   :color "#353B50"
                   :font-size "18px"
                   :padding "13px 20px"
                   :box-shadow "none"
                   :outline "none"
                   :border-bottom "1px solid #D8D8D8"}
     [:&::placeholder {:color "#979797"
                       :font-size "18px"}]]]
   [:&:focus {:background "transparent"
              :border-left "none"
              :border-right "none"
              :border-top "none"
              :box-shadow "none"
              :outline "none"
              :border-bottom "2px solid #white"}]
   [:a.item {:color "#696969"
             :display "block"
             :font-size "14px"
             :font-family "Roboto"
             :letter-spacing "0px"
             :line-height "30px"
             :padding "2px 5px"}
    [:&:hover {:text-decoration "none"
               :color "#FFFFFF"
               :background-color "#0079FF"}]
    [:.fas {:color "#3F455A"
            :width "1em"
            :margin-right "5px"}]]
   [:.categories
    {:display :flex
     :height "auto"}
    [:.optional-categories
     {:padding "0 30px"
      :padding-top "1px"
      :display :flex
      :line-height "16px"
      :flex-wrap :no-wrap
      :align-content :flex-start
      :border-radius "0 0 0 6px"
      :background-color "#EAF2FF"}
     [:.title-cat {:padding-bottom "7px"
                   :line-height "21px"}]]

    [:.stable-categories {:display :flex
                          :padding "0 60px 60px 15px"
                          :align-content :flex-start
                          :min-width "500px"}]
    [:.category
     {:width "170px"
      :padding "0 15px"}]]])

(defn nav-items []
  (let [nav (rf/subscribe [::model/navigation])]
    (fn []
      [:div.categories
       [:div.stable-categories
        (for [{items :items ttl :title mark :mark} @nav]
          (when-not (empty? items)
            (when-not (or (= mark "user")
                          (= mark "admin"))
              [:div.category {:key ttl}
               [:p.title-cat ttl]
               (for [i items]
                 [:a.item {:key (href i)
                           :on-click #(rf/dispatch [:off-navigation])
                           :href (:href i)}
                  #_[wgt/icon (:icon i)]
                  " "
                  (:text i)])])))]])))


(defn index [_]
  (let [nav-search (rf/subscribe [::model/navigation-search])]
    (fn []
      (let [{:keys [routes could-be-hided?]} @nav-search]
        [:div.navigation {:on-click (fn [e]
                                      (when (and could-be-hided? (= (.-target e) (.-currentTarget e)))
                                        (rf/dispatch [::model/toggle-navigation])))}
        (styles/style nav-style)
         [:div.menu-block
          [:div.search-bar
           [:div.space-for-button]
           [:input.nav-search {:type "search"
                               :auto-focus true
                               :value routes
                               :on-change #(rf/dispatch [::model/search (.. % -target -value)])
                               :on-focus (fn [e] (let [t (.. e -target -value)]
                                                   (set! (.. e -target -value) "")
                                                   (set! (.. e -target -value) t)))
                               :placeholder "Search"}]
           [:div.comment "Menu"]
           [nav-items]]]]))))

(def quick-nav-style
  [:.nav-cnt
   {:display "flex"
    :position "sticky"
    :top "0"
    :flex-direction "row"
    :justify-content "space-evenly"}
   [:.nav-bottom {:display "flex"
                  :background-color "#F9F9F9"
                  :margin-bottom "30px"
                  :max-height "40px"
                  :flex-direction "row"}]
   [:.nav-bar {:display "flex"
               :background-color "#F9F9F9"
               :width "100%"
               :justify-content "space-between"
               :margin-bottom "10px"
               :flex-direction "row"}]
   [:.groups {:display "flex"
              :flex-direction "row"}]
   [:.left-group {:display "flex"
                  :margin-left "20px"
                  :flex-direction "row"}]
   [:.right-group {:margin-right "20px"}]
   [:.group {:height "20px"
             :max-height "40px"
             :margin-right "8px"
             :border-right "1px solid #E8E8E8"
             :align-items "center"}
    [:&:last-child {:border-bottom "none"}]
    [:&.right {:border-right "none"}]]
   [:.arrow-left {:width 0
                  :height 0
                  :border-top "10px solid transparent"
                  :border-bottom "10px solid transparent"
                  :border-right "10px solid black"}]
   [:.arrow-right {:width 0
                   :height 0
                   :border-top "10px solid transparent"
                   :border-bottom "10px solid transparent"
                   :border-left "10px solid black"}]
   [:.icon {:height sidebar-width
            :color "#212529"
            :cursor "pointer"
            :display "flex"
            :justify-content "center"
            :align-items "center"
            :position "relative"
            :margin-left "0px"
            :width sidebar-width}
    [:&.last-updated {:text-align "center"
                      :font-size "12px"}]
    [:.popup {:display "none"}]
    [:&.burger {:z-index 201
                :width "120px"}]
    [:.popup-left {:display "none"}]
    [:&:hover
     [:.popup {:position "absolute"
               :display "flex"
               :align-items "center"
               :z-index 2
               :left (str "calc(" sidebar-width " - 10px)")}
      [:.text {:border-radius "4px"
               :font-size "14px"
               :border "none"
               :white-space "nowrap"
               :width "fit-content"
               :padding "5px 15px"
               :background-color "#000000"
               :color "white"}]]]
    [:&:hover
     [:.popup-left {:position "absolute"
                    :display "flex"
                    :align-items "center"
                    :z-index 2
                    :right (str "calc(" sidebar-width " - 10px)")}
      [:.text {:border-radius "4px"
               :font-size "14px"
               :border "none"
               :white-space "nowrap"
               :width "fit-content"
               :padding "5px 15px"
               :background-color "#000000"
               :color "white"}]]]
    [:&.active {:justify-content "space-between"
                :background-color "#EFF6FF"}]
    [:.selector {:width "3px"
                 :height "100%"}
     [:&.left {:background-color "#017AFF"}]]]])

(def bottom-group
  [[:a {:href "#/how-to-use"
        :target "_blank"
        :on-click #(rf/dispatch [:off-navigation])}
    [:div.icon
     [:div.popup [:div.arrow-left] [:div.text "How to use"]]
     [:div.navigation-documentation-icon]]]

   [:a {:href "#/buy-subscription"
        :target "_blank"}
    [:div.icon
     [:div.popup [:div.arrow-left] [:div.text "Subscribe"]]
     [:div.without-check-icon]]]

   [:a {:href "#"
        :target "_blank"
        :on-click #(rf/dispatch [:auth/signout])}
    [:div.icon
     [:div.popup-left [:div.text "Sign Out"] [:div.arrow-right]]
     [:div.navigation-exit-icon]]]])

(defn quick-nav [_]
  (let [{:keys [qnav]} @(rf/subscribe [::model/quick-nav])
        nav (rf/subscribe [::model/nav-active])]
    (fn []
      [:div.nav-cnt
       [:div.nav-bar
        [:div.left-group
         [:div.groups
          (for [[idx group] (map-indexed vector qnav)
                :let [{:as i :keys [active?]} group]] ^{:key idx}
            [:div.group
             [:a {:href (:href i)
                  :on-click #(rf/dispatch [::model/off-navigation])
                  :key (:href i)}
              [:div.icon
               [:div.popup [:div.arrow-left] [:div.text (:text i)]]
               [:div {:class (str "navigation-" (:icon i) "-icon")}]]]])]]
        [:div.right-group
         [:div.nav-bottom
          [:div.groups
           (for [g bottom-group]
             ^{:key ()}
             [:div.group g])]]]]])))

(pages/reg-page :navigation/index index)
