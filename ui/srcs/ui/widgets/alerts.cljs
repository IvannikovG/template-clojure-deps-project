(ns ui.widgets.alerts
  (:require [re-frame.core :as rf]
            [garden.core :as garden]
            [garden.units :as u]
            [reagent.core :as r]))

;; TODO styles should be local to this component

;; this is done to prevent class name clash
(def selector "ui.widgets.alerts")

(def common-stylo "
  border: 1px solid #AFAFAF;
  font-size: 16px;
  padding: 10px;
  color: #0087BC;
  border-radius: 12px;
  background-color: #E3F6FE;
  position: fixed;
  bottom: 40px;
  right: 10px;")

(defn popup [class]
  (fn [cfg]
    (let [text (or (:text cfg) cfg)
          duration (* (or (:duration cfg) 5) 1000)
          root (.getElementById js/document "app")
          messagebox (.createElement js/document "div")]
      (when-let [old-item (.item (.getElementsByClassName js/document selector) 0)]
        (.removeChild root old-item))
      (.setAttribute messagebox "class" (str selector " alert " class))
      (.setAttribute messagebox "style" common-stylo)
      (set! (.-innerHTML messagebox) text)
      (.appendChild root messagebox)
      (js/setTimeout (fn [_]
                       (when (.-parentNode messagebox)
                         (.removeChild root messagebox)))
                     duration))))

;; todo remove namespaced keywords or support jvm implementation
(rf/reg-fx
 ::success
 (popup "alert-success"))

(rf/reg-fx
 :alerts/success
 (popup "alert-success"))

(rf/reg-fx
 ::fail
 (popup "alert-danger"))

(rf/reg-fx
 :alerts/fail
 (popup "alert-danger"))
