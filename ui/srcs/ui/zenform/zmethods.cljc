(ns ui.zenform.zmethods)

(defmulti init-input
  (fn [key schema value] key))
