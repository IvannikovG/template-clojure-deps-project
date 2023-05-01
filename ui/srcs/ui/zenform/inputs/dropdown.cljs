(ns ui.zenform.inputs.dropdown
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn widget [{fp :form-path p :path on-change :on-change}]
  (let [state (r/atom nil)]

    (letfn [(toggle [] (swap! state update :open? not))
            (close [] (swap! state assoc :open? false))
            (open? [] (:open? @state))
            (set-root [node] (swap! state assoc :root node))
            (get-root [] (:root @state))
            (set-value [val]
              (if on-change (on-change val) (rf/dispatch [:zenform/on-change fp p val])))
            (doc-handler [e]
              (when-not (.contains (get-root) (.-target e))
                (close)))
            (set-doc-handler []
              (.addEventListener (.-body js/document) "click" doc-handler))
            (unset-doc-handler []
              (.removeEventListener (.-body js/document) "click" doc-handler))]

      (r/create-class

       {:component-did-mount
        (fn [this]
          (set-root (r/dom-node this))
          (set-doc-handler))

        :component-will-unmount
        (fn [this]
          (unset-doc-handler))

        :reagent-render
        (fn [{:keys [data] :as props}]
          (let [value (if data  (:value data)
                          (:value props))
                err  (or (:errors data) (:errors props))]
            [:div.zen-select
             [:div.select
              {:tabIndex 0
               :on-click #(toggle)
               :class (when err "invalid")}
              [:span.triangle "▾"]
              (if value
                [:span.value (or (:display value) value)]
                [:span.choose-value (or (:placeholder props) "Select ...")])
              (when (and value (not (:disable-cross props)))
                [:i.material-icons.cross
                 {:on-click
                  (fn [e]
                    (.stopPropagation e)
                    (set-value nil)
                    (close))} "close"])]
             (cond
               (open?) [:div.input-options
                        [:div.options.no-input
                         {:style {:top :37px}}
                         [:div
                          (for [option (:options props)]
                            ^{:key option}
                            [:div.option
                             {:on-click
                              (fn []
                                (set-value option)
                                (close))}
                             (or (:display option) option)])]]]
               (and err (not (:disable-errors props))) [:div.err-msg (first (vals err))]
               )]))}))))
