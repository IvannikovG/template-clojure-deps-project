(ns ui.zenform.inputs.select
  (:require
   [clojure.string :as str]
   [ui.utils :refer [to-int]]
   [goog.functions :refer [debounce]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.set :as set]))

;; in our case inner search would be faster than outer
(defn is-child? [par child]
  (if (.isEqualNode par child)
    true
    (some identity
          (map #(is-child? % child)
               (array-seq (.-childNodes par))))))

(defn child [parent-node classname]
  (aget (.getElementsByClassName parent-node classname) 0))

(defn children [parent-node & classnames]
  (flatten (map #(array-seq (.getElementsByClassName parent-node %)) classnames)))

(defn swap-in-vec [v idx1 idx2]
  (-> v
      (assoc idx1 (get v idx2))
      (assoc idx2 (get v idx1))))

(defn drop-everything-starting-from
  [string symb]
  (reduce
   (fn [acc el]
     (if (= (str el) symb)
       (reduced acc)
       (str el acc)))
   ""
   string))

(defn- tag [{:as data :keys [values set-value idx display on-delete draggable readonly]}]
  [:span.tag
   (when draggable
     {:onDragStart (fn [ev] (.setData ev.dataTransfer "text" idx))
      :onDrop (fn [ev]
                (.preventDefault ev)
                (->> (.getData ev.dataTransfer "text")
                     to-int
                     (swap-in-vec values idx)
                     set-value))
      :onDragOver (fn [ev] (.preventDefault ev))
      :draggable "true"})
   [:span display]
   (when (not readonly)
     [:i.cross.material-icons
     {:on-click on-delete} "close"])])


(defn widget [{:keys [form-path path search items loading multi-values? auto-focus html-id
                      on-change value draggable readonly
                      search-by-enter search-hint-text debounce-interval add-items? max-item-len]
               :as opts}]
  (let [items (rf/subscribe items)
        loading (when loading (rf/subscribe loading))
        state (r/atom (cond-> {:active false} value (assoc :value value)))
        doc-click-listener (fn [e]
                             (let [outer-click? (not (is-child? (:root-node @state) (.-target e)))]
                               (when (and outer-click? (:active @state))
                                 (when-let [f (:on-blur opts)] (f e))
                                 (swap! state assoc :active false))))
        on-search (cond
                    search-by-enter identity
                    debounce-interval (debounce #(rf/dispatch (conj search %)) debounce-interval)
                    :else #(rf/dispatch (conj search %)))
        key-handler (fn [e]
                      (when (and search-by-enter (= 13 (.-keyCode e)))
                        (rf/dispatch (conj search (.. e -target -value)))))
        open-popup (fn [_]
                     (when-not readonly
                       (if (:active @state)
                         (swap! state assoc :active false)
                         (do
                           (js/setTimeout #(.focus (:input-node @state)) 10)
                           (on-search (.-value (:input-node @state)))
                           (swap! state assoc :active true)))))
        outer-key-handler (fn [ev]
                            (when-not (or (#{46 8 9 18} (.-keyCode ev)) (.-altKey ev))
                              (open-popup ev)
                              (.preventDefault ev)))
        options-key-handler (fn [ev]
                              (let [k (.-keyCode ev)]
                                (cond
                                  (= 13 k) (.click (.-target ev))
                                  (= 38 k) (do (when-let [i (.. ev -target -previousSibling)]
                                                 (.focus i))
                                               (.preventDefault ev))
                                  (= 40 k) (do (when-let [i (.. ev -target -nextSibling)]
                                                 (.focus i))
                                               (.preventDefault ev))
                                  (#{9 16} k) nil
                                  :default (swap! state assoc :active false))))

        set-value (fn [val]
                    (swap! state assoc :value val)
                    (when on-change (on-change val))
                    (when (and form-path path)
                      (rf/dispatch [:zenform/on-change form-path path val])))

        will-recv-props (fn [v]
                          (swap! state assoc :value v)
                          v)

        handle-display (fn [display]
                         (if (and max-item-len (> (count display) max-item-len))
                           (str (apply str (take (- max-item-len 3) display)) "...")
                           display))]

    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [root (r/dom-node this)]
          (swap! state assoc :root-node root)
          (when-let [options-node (child root "options")]
            (swap! state assoc :options-node options-node)
            (.addEventListener options-node "keydown" options-key-handler))
          (when-let [focus-node (child root "select")]
            (swap! state assoc :focus-node focus-node)
            (.addEventListener focus-node "keydown" outer-key-handler)
            (when auto-focus
              (js/setTimeout #(.focus focus-node) 0)))
          (when-let [input (child root "zen-search-input")]
            (swap! state assoc :input-node input)
            (.addEventListener input "keydown" key-handler)))
        (.addEventListener js/document "click" doc-click-listener))

      :component-will-receive-props
      (fn [_ nextprops]
        (when-let [{v :value} (second nextprops)]
          (will-recv-props v)))

      :component-will-unmount
      (fn [this]
        (when-let [options-node (:options-node @state)]
          (.removeEventListener options-node "keydown" options-key-handler))
        (when-let [focus-node (:focus-node @state)]
          (.removeEventListener focus-node "keydown" outer-key-handler))
        (when-let [input (:input-node @state)]
          (.removeEventListener input "keydown" key-handler))
        (.removeEventListener js/document "click" doc-click-listener))

      :reagent-render
      (fn [props]
        (let [data (:data props)
              {:keys [value]} data
              value (or value (:value props))]
          [:div.zen-select
           [:div.select
            {:class (when (:errors data) "invalid")
             :id html-id
             :tab-index "0"
             :on-click open-popup}
            [:span.triangle "▾"]
            (cond
              (and multi-values?
                   (not-empty value)) (let [bad-string-value? (and (string? value)
                                                                   (str/includes? value "{"))
                                            distinct-values (remove #(= % ",") (distinct value))
                                            string-version? (and
                                                             (->> distinct-values (some string?))
                                                             (->> distinct-values (some map?)))]
                                        (cond
                                          bad-string-value?
                                          (let [strings (drop-everything-starting-from distinct-values "{")]
                                            [:span.value
                                             (for [[idx v] (map-indexed vector strings)] ^{:key idx}
                                               [tag {:on-delete (fn [e]
                                                                  (->> strings
                                                                       (remove (partial = v))
                                                                       vec
                                                                       set-value)
                                                                  (.stopPropagation e))
                                                     :idx idx
                                                     :readonly (:readonly props)
                                                     :values strings
                                                     :set-value set-value
                                                     :draggable draggable
                                                     :display (handle-display (or (:short-display v) (:display v) (str v)))}])])

                                          string-version?
                                          (let [strings (remove map? distinct-values)
                                                new-value (->> value (filter map?)
                                                               first :short-display)
                                                value-is-new (contains? (set strings) new-value)
                                                strs (if value-is-new
                                                       (distinct (concat strings new-value))
                                                       (distinct strings))]
                                            [:span.value
                                             (for [[idx v] (map-indexed vector strs)] ^{:key idx}

                                               [tag {:on-delete (fn [e]
                                                                  (->> strs
                                                                       (remove (partial = v))
                                                                       vec
                                                                       set-value)
                                                                  (.stopPropagation e))
                                                     :idx idx
                                                     :readonly (:readonly props)
                                                     :values strs
                                                     :set-value set-value
                                                     :draggable draggable
                                                     :display (handle-display (or (:short-display v) (:display v) (str v)))}])])
                                          :else
                                          [:span.value
                                           (for [[idx v] (map-indexed vector distinct-values)] ^{:key idx}
                                             [tag {:on-delete (fn [e]
                                                                (->> value
                                                                     (remove (partial = v))
                                                                     vec
                                                                     set-value)
                                                                (.stopPropagation e))
                                                   :idx idx
                                                   :readonly (:readonly props)
                                                   :values value
                                                   :set-value set-value
                                                   :draggable draggable
                                                   :display (handle-display (or (:short-display v) (:display v) (str v)))}])]))

              (and (not multi-values?) value) [:span.value (or (handle-display (or (:short-display value) (:display value))) value)]
              :else [:span.choose-value (or (:placeholder props) "Select...")])

            (when (and (not multi-values?) (not readonly) value (not (:disable-cross props)))
              [:i.material-icons.cross
               {:on-click (fn [e]
                            (set-value nil)
                            (set! (.-value (:input-node @state)) "")
                            (.stopPropagation e))}
               "close"])]

           (when-let [err (and (not (:disable-errors props)) (:errors data))]
             [:div.err-msg (first (vals err))])
           [:div.input-options
            {:style {:display (if (:active @state) :block :none)}}
             [:input.zen-search-input
              {:tab-index 0

               :class (when-not (:active @state) :inactive)
               :value (:search-input @state)
               :on-change (fn [ev]
                            (let [v (.. ev -target -value)]
                              (swap! state assoc :search-input v)
                              (on-search v)))}]
             (when (and loading (:active @state) @loading) [:div "..."])
            [:div.options
             (let [hint (str search-hint-text " " (when search-by-enter "(press enter for lookup)"))]
               (when-not (str/blank? hint) [:div.info-left hint]))
             (cond
               (and (empty? @items) add-items? (some? (:input-node @state))
                    (not-empty (.-value (:input-node @state))))
               (let [v (.-value (:input-node @state))]
                 [:div.add
                  {:tab-index (if (:active @state) "0" "-1")
                   :on-click (fn [e]
                               (swap! state assoc :active false)
                               (swap! state assoc :search-input "")
                               (set-value v))}
                  (str "Add " v)])
               (empty? @items)
               [:div.info "No results"]
               :else
               (doall
                (for [opt @items]
                  [:div.option
                   (cond->
                     {:key (pr-str opt)
                      :tab-index (if (:active @state) "0" "-1")
                      :on-click (fn [_]
                                  (swap! state assoc :active false)
                                  (swap! state assoc :search-input "")
                                  (set-value (if multi-values? (conj (vec value) opt) opt)))}
                     max-item-len
                     (assoc :title (:display opt)))
                   (or
                    (handle-display (:display opt))
                    opt)])))]]]))})))

