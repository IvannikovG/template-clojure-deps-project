(ns ui.widgets
  (:require
   [re-frame.core :as rf]
   [garden.units :as u]
   [ui.styles :as styles]
   [reagent.core :as r]
   [cljs.pprint :as pp]
   [ui.routes :refer [href back]]
   [clojure.string :as str]))

(def action-styles
  (styles/style
   ;; TODO rename back to actions when styles be unified
   [:.buttons
    {:display :flex
     :flex-direction :row
     :justify-content "flex-start"
     :align-items :center}
    [:a
     {:position "relative"
      :height "35px"
      :padding "5px 40px"
      :margin-right "12px"
      :transition-duration "100ms"
      :transition-timing-function "ease-in"
      :border "1px solid transparent"
      :text-decoration "none"
      :color "#fff"}]
    [:.loader
     {:font-size "4px" :margin "0px" :position "absolute" :top "7px" :right "7px"}]
    [:a.primary
     {:background-color "#4a90e2"}
     [:&:hover {:background-color "#0C7AD9"}]]
    [:a.disabled
     {:cursor :not-allowed
      :opacity "0.5"}]
    [:a.secondary {:border "1px solid #4a90e2" :color "#4a90e2"}
     [:&:hover {:background-color "#4a90e2" :color "#fff"}]]
    [:a.danger {:background-color "#CC0000"
                :opacity 0.3}
     [:&:hover {:opacity 1}]]]))

(rf/reg-sub
 ::form-fetching
 (fn [db [_ fetch-path]]
   (get-in db fetch-path)))

(defn pagination
  [page-links-data]
  [:div.pagination-widget
   (styles/style
    [:.pagination-widget {:display :flex
                          :justify-content :center
                          :font-size "16px"
                          :margin "10px 0 10px 0"}
     [:&--link {:display :block
                :padding "2px 4px"
                :margin "0 4px"
                :text-decoration :none
                :border-radius "3px"
                :color "#979797"
                :font-weight 500}
      [:&:hover {:text-decoration :none
                 :color "#979797"
                 :background-color "#F3F3F3"}]
      [:&_current {:background-color "#EFF6FF"
                   :color "#017AFF"}]]
     [:&--offset {:display :block
                  :font-weight 500
                  :color "#979797"
                  :padding "2px 1px"}]])
   (for [{:keys [page url offset current]} page-links-data]
     [:<>
      {:key page}
      (when offset [:span.pagination-widget--offset "..."])
      [:a.pagination-widget--link
       {:href url
        :class (str (when current "pagination-widget--link_current"))}
       page]])])

(defn icon [nm & [title]]
  (when nm
    [:i.fas {:title title
             :class (str "fa-" (name nm)) :aria-hidden true}]))

(defn btn-loading?
  [btn]
  (when-let [path (:loading btn)]
    @(rf/subscribe
      [::form-fetching path])))

(defn btn-spinner []
  [:div.loader.grid-spinner])

(defn actions [& buttons]
  (let [buttons (filter identity buttons)]
    [:div.buttons
     action-styles
     (doall
      (for [b buttons
            :let [loading? (btn-loading? b)
                  {:keys [label href on-click type disabled? target style]} b]]
        [:a {:key label
             :target target
             :style style
             :href (or href "#")
             :on-click (when on-click (fn [e]
                                        (if (or loading? disabled?)
                                          (.preventDefault e)
                                          (when-let [f on-click]
                                            (.preventDefault e)
                                            (f)))))
             :class (str (or type "secondary") (when (or loading? disabled?) " disabled"))}
         label
         (when loading? [btn-spinner])]))]))
