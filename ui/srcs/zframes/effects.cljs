(ns zframes.effects
  (:require [re-frame.core :as rf]))

(rf/reg-fx
 :effects/set-title
 (fn [value]
   (set! (.-title js/document) (or (not-empty value) "Chinese Studio"))))

(rf/reg-fx
 :focus-by-id
 (fn [id]
   (.. js/document (getElementById id) focus)))


(rf/reg-fx
 :copy-to-clipboard
 (fn [[text-to-copy element-to-highlight]]
   ;; (prn text-to-copy element-to-highlight)
   (if-not element-to-highlight
     (->
      (js/navigator.clipboard.writeText text-to-copy)
      (.catch (fn [e] (throw (js/Error. "copy to clipboard failed")))))
     (->
      (js/navigator.clipboard.writeText text-to-copy)
      (.then (js/Promise.   #(.classList.add    element-to-highlight "highlighted")))
      (.then (js/setTimeout #(.classList.remove element-to-highlight "highlighted") 200))
      (.catch (fn [e] (throw (js/Error. "copy to clipboard failed"))))))))

(rf/reg-event-fx
 :copy-to-clipboard
 (fn [{:keys [db]} [_ x y]]
   (let [[text-to-copy
          element-to-highlight] (cond
                                  y [x y]
                                  (string? x) [x nil]
                                  :else [(.-text x) x])]
     {:copy-to-clipboard [text-to-copy element-to-highlight]})))
