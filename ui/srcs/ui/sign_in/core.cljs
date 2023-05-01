(ns ui.sign-in.core
  (:require
   [re-frame.core :as rf]
   [ui.styles :as styles]
   [ui.widgets :as wgt]
   [ui.zenform.core :as zf]
   [ui.sign-in.model :as m]
   [ui.pages :as pages]))

(def styles
  [:.form
   {:width "150px"
    :margin "100px auto"
    :display "flex"
    :flex-direction "column"
    :align-items "center"}
   [:.sign-in-button {:width "80px"
                      :padding-left "2px"
                      :margin-top "10px"
                      :padding-top "2px"
                      :padding-bottom "2px"
                      :text-align "center"
                      :color      "white"
                      :border-radius "14px"
                      :background-color "#4B84FF"
                      :height "20px"}]
   [:.zen-select {:width "150px"}
    [:.input-options [:.options {:max-height "300px"}]]
    [:.select {:border "1px solid #cecece"
               :background-color "rgb(244, 245, 247)"}
     [:.choose-value {:color "gray"}]]]
   [:.form-row {:margin-top       "10px"}
    [:&.location {:margin-top "10px"
                  :width "150px"}]
    [:.err-msg {:color "rgb(217, 86, 64)"
                :font-size "12px"}]
    [:input {:padding "2px 8px" :border "1px solid #cecece"}]]
   [:.buttons {:margin-top "15px"}]
   [:.error {:margin-top "10px" :color "rgb(217, 86, 64)"}]])

(defn sign-in [_]
  (let [m @(rf/subscribe [m/page-name])]
    [:div.form
     [styles/style styles/form-styles]
     (styles/style styles)

     [:h3 "Sign In"]
     [:div.form-row [zf/i {:id :input :form-path m/fp :path [:username]}]]
     [:div.form-row [zf/i {:id :input :form-path m/fp :path [:password]}]]

     [:div.sign-in-button
      {:on-click #(rf/dispatch [:zenform/action m/fp :submit])}
      "Sign in"]
     [:div.form-row
      [:a {:href "#/register"} "Register"]]
     (when-let [{m :error} (:errors m)]
       [:div.error m])]))

(pages/reg-page m/page-name sign-in)
