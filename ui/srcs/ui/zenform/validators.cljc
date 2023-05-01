(ns ui.zenform.validators
  (:require
   [clojure.string :as str]))

(defmulti validate (fn [validator-name _] validator-name))

(defn not-empty-value [v]
  (not (cond
         (string? v) (str/blank? v)
         (coll? v) (empty? v)
         :else (nil? v))))

(defn maybe-two-digits-float [v]
  (if (number? v)
    (< (-> v str (str/split #"\.") second count) 3)
    true))

;; TODO add default messages and their override
(def validators
  {:v/maybe-two-digits-float {:f       maybe-two-digits-float
                              :message "Too many digits after dot"}
   :v/email                  {:f       #(re-matches #".+\@.+\..+" %)
                              :message "Not an email"}
   :v/required               {:f       not-empty-value
                              :message "Should not be blank"}
   :v/natural                {:f       #(when (number? %) (pos? %))
                              :message "Should be greater than 0"}
   :v/maybe-not-neg          {:f       #(or (nil? %) (when (number? %) (pos? %)))
                              :message "Should be greater than 0"}
   :v/number                 {:f       number?
                              :message "Should be a number"}
   :v/maybe-number           {:f       #(or (nil? %) (number? %))
                              :message "Should be a number"}
   :v/maybe-not-zero         {:f       #(or (nil? %) (when (number? %) (not (zero? %))))
                              :message "Must be correct number and not zero"}
   :v/non-negative-number    {:f       #(when (number? %) (>= % 0))
                              :message "Should be a non-negative number"}
   :v/unsupported-symbols-for-id {:f (fn [s] (not (or (str/includes? (str s) "/")
                                                      (str/includes? (str s) "&"))))
                                  :message "Should not contain: '/', '&'"}})

