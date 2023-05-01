(ns ui.zenform.inputs.textarea
  (:require
   [re-frame.core :as rf]
   [ui.styles :as styles]
   [garden.units :as u]
   [clojure.string :as str]
   [ui.widgets :as wgt]
   [ui.pages :as pages]
   [reagent.core :as r]
   [ui.routes :as routes]
   [ui.utils :as utils]))

(def style
  (styles/style
   [:.zen-textarea {:border "1px solid #DADCE0"
                    :border-radius "30px"
                    :width "400px"
                    :margin "10px"}
    [:.z-text {:display "flex"
               :resize "none"
               :border "none"
               :border-radius "30px"
               :font-size "18px"
               :font-family "sans-serif"
               :letter-spacing "0.3px"
               :padding "8px 16px"}
               [:&:placeholder {:color "red"}]]]))

(defn widget [{fp :form-path p :path pl :placeholder data :data}]
  (let [err (:errors data)]
    [:div.zen-textarea style
     [:textarea.z-text
      {:value (:value data)
       :placeholder pl
       :class (when (:errors data) "invalid")
       :on-change #(rf/dispatch [:zenform/on-change fp p (.. % -target -value)])}]
     (when-let [err (:errors data)]
       [:div.err-msg (first (vals err))])]))
