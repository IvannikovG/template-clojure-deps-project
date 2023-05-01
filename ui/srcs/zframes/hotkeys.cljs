(ns zframes.hotkeys
  (:require [re-frame.core :as rf]
            [re-frame.db :as db]))

(defonce handlers (atom {"Alt+Slash" {:event ::help}}))

(rf/reg-event-fx
 ::help
 (fn [& _] (println @handlers)))

;; @handlers

;; Pattern:
;; Ctrl + Shift + Alt + _

(rf/reg-fx
 :hotkey/init
 (fn [opts]
   (swap! handlers merge opts)
   (.addEventListener
    js/document
    "keydown"
    (fn [ev]
      (let [k (str (when (.-ctrlKey  ev) "Ctrl+")
                   (when (.-altKey   ev) "Alt+")
                   (when (.-shiftKey ev) "Shift+")
                   (.-code ev))]
        (when-let [h (get @handlers k)]
          (.preventDefault ev)
          ;; Andrey - for dispatching multiargument events, such as :zenform/action etc.
          (rf/dispatch (let [event-args (:event-args h)]
                         (if (vector? event-args)
                           (into [(:event h)] event-args)
                           [(:event h) h])))))
      ) false)))

(rf/reg-fx
 :hotkey/add
 (fn [opts]
   (swap! handlers (fn [old] (reduce (fn [ks [id ev]] (assoc ks (:key ev) (assoc ev :id id))) old opts)))))

(rf/reg-fx
 :hotkey/clear
 (fn [id]
   (let [ids (into #{} (if (sequential? id) id #{id}))]
     (swap! handlers (fn [old] (reduce (fn [ks [k ev]] (if (contains? ids (:id ev)) (dissoc ks k) ks)) old old))))))
