(ns ui.zenform.inputs.input
  (:require
   [clojure.string :as str]))

(defn build-classes [mapping]
      (->> (filter second mapping)
           (map first)
           (str/join " ")))

(defn widget [{:keys [default value on-change on-enter-down
                      placeholder errors disable-errors type readonly]}]
  (let [on-key-down (fn [ev]
                      (when (= 13 (.-which ev))
                        (let [v (.. ev -target -value)]
                          (on-enter-down v))))]
    [:div.zen-input
     [:input
      (cond-> {:value (or (:display value) value)
               :placeholder placeholder
               :type type
               :readOnly readonly
               :class (build-classes {"invalid" errors
                                      "readonly" readonly})
               :on-change #(on-change (let [v (.. % -target -value)]
                                        (if (and default (str/blank? v)) default v)))}
        on-enter-down
        (merge {:on-key-down on-key-down}))]
     (when-let [err (and (not disable-errors) errors)]
       [:div.err-msg (first (vals err))])]))
