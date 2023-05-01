(ns ui.likes.core
  (:require
   [ui.styles :as styles]
   [ui.zenform.core :as zfc]
   [ui.utils :as u]
   [ui.widgets :as wgt]
   [ui.routes :as routes]
   [re-frame.core :as rf]
   [ui.pages :as pages]
   [ui.likes.model :as m]
   [clojure.string :as str]))


(def dashboard-styles
  [:.dashboard
   {:width "100%"}
   [:.tag-lines {:display "flex"
                 :flex-direction "column"
                 :justify-content "flex-start"}]
   [:.tagged {:display "flex"
              :flex-direction "row"}
    [:.tagged-header {:margin-right "5px"}]
    [:.tagged-items {:font-size     "16px"
                     :overflow      "hidden"
                     :text-overflow "ellipsis"}]]
   [:.main-header {:font-size "24px"
                   :font-weigth "semibold"
                   :margin-bottom "10px"}]
   [:.general-line {:display "flex"
                    :padding-top "6px"
                    :background-color "#F9F9F9"
                    :margin-right "20px"
                    :width "100%"
                    :justify-content :space-between
                    :border-bottom "1px solid #E6E4E4"}]
   [:.taskwidget {:margin-top "5px"
                  :padding "4px"
                  :margin-left "6px"
                  :border-top-right-radius "6px"
                  :border-bottom-right-radius "6px"
                  :border-left "1px solid rgba(128, 196, 255, 0.8)"
                  :color "#292929"
                  :page-break-inside "avoid"
                  :break-inside "avoid-column"
                  :margin-bottom "5px"}
    [:.header-part {:display "flex"
                    :flex-direction "row"}]
    [:&:hover {:color "#4F80DA"}]]
   [:.linker {:text-decoration "none"
              :width "100%"
              :margin-bottom "4px"}]
   [:.deleter {:margin-right "12px"
               :margin-top   "10px"
               :color "flex"
               :border-bottom "1px solid #E6E4E4"}]
   [:.sessions {:margin-top "20px"
                :display "flex"
                :border-bottom "1px solid #E6E4E4"}
    [:.tasks {:display "flex"
              :flex-wrap "wrap"}]]

   [:.author {:color "#017AFF"
              :margin-right "30px"
              :padding "18px 22px 18px 22px"
              :min-width "250px"
              :max-width "250px"
              :white-space "nowrap"
              :overflow "hidden"
              :text-overflow "ellipsis"
              :max-height "50px"
              :background-color "#FFF"
              :box-shadow "-1px -1px 2px 0 rgba(0,0,0,0.06), 0 2px 6px 0 rgba(0,0,0,0.06)"
              :border-radius "8px"            :font-size "21px"
              :letter-spacing "-0.55px"
              :line-height "16px"}]])

(def style*
  (styles/style
   [dashboard-styles]))

(defn deleter [] [:div {:title "delete?"} "X"])

(defn dashboard [_]
  (let [{:keys [likes]} @(rf/subscribe [m/page-name])
        record-ids (str/join ", " (mapv (fn [el] (-> el :hieroglyph :resource :hieroglyph_id)) likes))]
    [:div.centered-content.dashboard
     style*
     (if (seq likes)
       [:div.main-header "Likes"]
       [:div.dashboard "No user likes created"])
     [:div.tag-lines
      (for [[idx t] (map-indexed vector likes)] ^{:key idx}
        (let [chinese   (-> t :hieroglyph :resource :chinese)
              s-link    (routes/href "cards" {:record-ids record-ids})]
          [:div.general-line
           [:a.linker {:href s-link}
            [:div.taskwidget
             [:div.tagged
              [:div.tagged-header "Liked: "]
              [:div.tagged-items chinese]]]]
           [:div.delete-like
            [zfc/button-with-confirm {:action   [::m/delete-like t]
                                      :label    [deleter]
                                      :label-ok "Yes"
                                      :label-no "No"}]]]))]]))

(pages/reg-page :likes/index dashboard)
