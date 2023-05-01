(ns ui.zenform.core
  (:require-macros [reagent.ratom :refer [reaction run!]])
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [garden.units :as u]
   [ui.zenform.validators :as v]
   [ui.zenform.model :as model]
   [ui.zenform.inputs.input :as input]
   [ui.zenform.inputs.textarea :as textarea]
   [ui.zenform.inputs.button-select :as button-select]
   [ui.zenform.inputs.select :as select]

   ;;[ui.zenform.inputs.calendar :as calendar]
   [ui.zenform.inputs.calendar-impl :as calendar]

   [ui.zenform.inputs.date-input :as date-input]
   [ui.zenform.inputs.radio :as radio]
   [ui.zenform.inputs.webcam :as webcam]
   [ui.zenform.inputs.dropdown :as dropdown]
   [ui.zenform.inputs.dropdown-tags :as dropdown-tags]
   [ui.zenform.inputs.tags :as tags]
   [ui.zenform.inputs.date-time :as date-time]
   [ui.zenform.inputs.switchbox :as switchbox]))

(def ->form-model model/->form-model)

(def i-map
  {:input input/widget

   :textarea textarea/widget
   :select select/widget
   :button-select button-select/widget
   :radio radio/widget

   ;; :date-input date-input/widget ;; never used, remove later

   :webcam webcam/widget

   ;; :calendar calendar/widget ;; never used

   :dropdown dropdown/widget
   :dropdown-tags dropdown-tags/widget
   :tags tags/widget

   :date date-time/date-input
   :date-dropdown date-time/date-input-dropdown
   :time date-time/time-input
   :date-time date-time/date-time-input
   :switchbox switchbox/switch-box})

;; TODO delete when convert to universal all widgets
(def converted-to-universal-widgets
  #{:input

    :date
    :time
    :date-time
    :switchbox})

(defn handle-string-input [v]
  (when-not (str/blank? v) v))

(defn i [{:keys [form-path path id] :as opts}]
  (if-let [wgt (get i-map id)]
    (if-let [data @(rf/subscribe [:zenform/form-path form-path path])]
      (if (converted-to-universal-widgets id)
        [wgt (merge (:widget data)
                    (dissoc opts :form-path :path)
                    ;;data ;; maybe get all keys from data?
                    {:on-change #(rf/dispatch [:zenform/on-change
                                               form-path
                                               path
                                               (cond-> %
                                                 (string? %)
                                                 handle-string-input)])
                     :value (:value data)
                     :errors (:errors data)})]
        [wgt (merge (:widget data)
                    opts
                    {:data data})])
      [:span])
    [:span (str "No widget " id " found")]))

(defn raw-wgt [{:keys [id] :as opts}]
  (if-let [wgt (get i-map id)]
    [wgt opts]
    [:span (str "No widget " id " found")]))

;; FIXME move to better place (namespace)
(defn button-with-confirm [opts]
  (let [state (r/atom nil)]
    (fn [{:keys [action label label-ok label-no]
          :or {label-ok "OK" label-no "Cancel"}} opts]
      (case @state
        :question [:span
                   {:style {:white-space :nowrap}}
                   [:a {:on-click
                        (fn [e]
                          (.stopPropagation e)
                          (reset! state nil))} label-no]
                   " / "
                   [:a {:on-click
                        (fn [e]
                          (.stopPropagation e)
                          (rf/dispatch action))} label-ok]]
        [:a {:on-click
             (fn [e]
               (.stopPropagation e)
               (reset! state :question))} label]))))
