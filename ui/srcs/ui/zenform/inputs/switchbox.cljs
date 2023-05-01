(ns ui.zenform.inputs.switchbox
  #_(:require
   [reagent.core :as reagent]
   [garden.units :as u]
   [re-frame.core :as rf]))


(defn switch-box [_]
  (fn [{:keys [value on-change label]}]
    (let [local-onchange (partial on-change (not value))]
      [:a.re-switch
       {:href "#"
        :class (when value :re-checked)
        :on-key-press (fn [event] (when (= 32 (.-which event)) (local-onchange)))
        :on-click (fn [e] (.preventDefault e) (local-onchange e))}
       [:span.re-switch-line [:span.re-box]]
       (when label [:span.re-label label])])))

