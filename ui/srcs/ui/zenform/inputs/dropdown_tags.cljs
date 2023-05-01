(ns ui.zenform.inputs.dropdown-tags
  (:require
   [clojure.set :as set]
   [reagent.core :as r]
   [re-frame.core :as rf]))


(defn- tag [label on-delete]
  [:span.tag
   [:span.label label]
   [:i.cross.material-icons
    {:on-click on-delete} "close"]])

(defn widget [{fp :form-path p :path}]
  (let [state (r/atom nil)]

    (letfn [(toggle [] (swap! state update :open? not))
            (close [] (swap! state assoc :open? false))
            (open? [] (:open? @state))
            (set-root [node] (swap! state assoc :root node))
            (get-root [] (:root @state))
            (set-value [val] (rf/dispatch [:zenform/on-change fp p val]))
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
        (fn [props]
          (let [data (:data props)
                {:keys [value]} data
                errs (:errors data)]
            [:div.zen-select
             [:div.select
              {:tabIndex 0
               :class (when errs "invalid")
               :on-click #(toggle)}
              [:span.triangle "▾"]
              (if (not-empty value)
                (for [v value] ^{:key (str v)}
                  [tag (or (:display v) v) #(do
                                              (set-value (set/difference (set value) (set [v])))
                                              (.stopPropagation %))])
                [:span.choose-value (or (:placeholder props) "Select ...")])]

             (cond
               (open?)
               [:div.input-options
                [:div.options.no-input
                 {:style {:top :37px}}
                 [:div
                  (for [option (:options props)
                        :let [selected? (contains? (set value) option)]]
                    ^{:key option}
                    [:div.option
                     {:class (when selected?
                               "selected")
                      :on-click #(if selected?
                                   (set-value (disj (set value) option))
                                   (set-value (conj (set value) option)))}
                     (or (:display option) option)
                     (when selected?
                       [:span.check-mark "✓"])])]]]

               errs
               [:div.err-msg (first (vals errs))])]))}))))
