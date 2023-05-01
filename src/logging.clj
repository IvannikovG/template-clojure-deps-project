(ns logging
  (:require [clojure.pprint :as pprint]))

(defn easy-logger
  [& arg]
  (pprint/pprint arg))
