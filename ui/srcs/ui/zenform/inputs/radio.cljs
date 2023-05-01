(ns ui.zenform.inputs.radio
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn widget [{fp :form-path p :path items :items
               data :data on-change :on-change :as opts}]
  (let [v (or (:value data) (:value opts))]
    (when (and (not-empty data) (nil? v))
      (rf/dispatch [:zenform/on-change fp p (first items)]))
    [:div.zen-radio
     (for [i items]
       [:div.option
        {:class (when (= i v) "active")
         :key (pr-str i)
         :on-click (if on-change
                     #(on-change i)
                     #(rf/dispatch [:zenform/on-change fp p i]))}
        [:span.radio (when (= i v) [:div.inner-radio])]
        [:span.value (or (:display i) (pr-str i))]
        [:span.desc  (:desc i)]])]))
