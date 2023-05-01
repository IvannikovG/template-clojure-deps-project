(ns zframes.cookies
  (:refer-clojure :exclude [get set!])
  (:require [goog.net.cookies :as gcookies]
            [re-frame.core :as rf]
            [cljs.reader :as reader]))

(rf/reg-cofx
 :cookie/get
 (fn [coeffects k]
   (let [ck (reader/read-string (or (.get goog.net.cookies (name k)) "nil"))]
     (assoc-in coeffects [:cookie k] ck))))

(rf/reg-fx
 :cookie/set
 (fn [m]
   (doseq [[k v] (seq m)]
     (.set goog.net.cookies (name k) (pr-str v) (* 60 60 10)))))

(rf/reg-fx
 :cookie/remove
 (fn [{k :key}]
   (.remove goog.net.cookies (name k))))

