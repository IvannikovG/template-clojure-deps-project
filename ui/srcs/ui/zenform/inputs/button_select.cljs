(ns ui.zenform.inputs.button-select
  (:require
   [re-frame.core :as rf]))

(defn widget [{:keys [form-path path items data on-change value]}]
  [:div.zen-button-select
   {:class (when (:errors data) "invalid")}
   (doall
    (for [i items]
      [:div.option
       {:key (pr-str i)
        :class (when (= i (if data (:value data) value)) "active")
        :on-click #(if on-change (on-change i) (rf/dispatch [:zenform/on-change form-path path i]))}
       [:span.value (or (:display  i) i)]]))])
