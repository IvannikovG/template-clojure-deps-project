(ns ui.zenform.inputs.tags
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.set :as set]
   [ui.styles :as styles]
   [clojure.string :as str]))

(def styles
  (styles/style
   [:.zen-tags
    {:position "relative" :display "flex" :align-items "center"}
    [:.empty-tags
     {:text-align "center" :color "#888"}]
    [:.zen-tag
     {:display :inline-flex
      :align-items :center
      :border "1px solid #ebebee"
      :border-radius "14px"
      :padding "2px 7px 2px 12px"
      :margin-right "8px"}
     [:i {:font-size "16px" :margin-left "2px" :cursor "pointer" :color "#868996"}
      [:&:hover {:color "#000"}]]]
    [:input
     {:width "107px"
      :height "29px"
      :margin-left "10px" ;;"auto"
      ;; EBANIY CSS
      :padding-left "12px !important"}
     [:&:focus {:outline "none"}]]
    [:button
     {:height "29px"
      :padding-right "0px 10px"
      :background-color "#fff"
      :border "1px solid #ebebee"
      :color "#868996"
      :position "relative"
      :left "-2px"
      :transition "color 100ms ease-in"}
     [:&:hover {:color "#0C7AD9"}]]]))

(rf/reg-event-fx
 ::on-change
 (fn [{db :db} [_ fp p v]]
   (let [value (get-in db (into (conj fp :value) (conj p :value)))]
     (if-not (contains? (set (map str/lower-case value)) (str/lower-case v))
       {:dispatch [:zenform/on-change fp p (conj (vec value) v)]}
       {:db db}))))

(defn widget [{fp :form-path p :path :as props}]
  (let [state (r/atom {:input nil})
        on-change #(let [v (:input @state)]
                     (when (not (or (nil? v) (str/blank? v)))
                       (swap! state assoc :input nil)
                       (rf/dispatch [::on-change fp p v])))
        on-keydown #(when (= (.-keyCode %) 13) (on-change))]
    (fn [props]
      (let [{:keys [value disable-errors errors]} (:data props)]
        [:div
         styles
         [:div.zen-tags
          (if (not-empty value)
            (for [v value]
              [:span.zen-tag {:key (str v)}
               [:span (or (:display v) v)]
               [:i.cross.material-icons
                {:on-click (fn [_] (rf/dispatch [:zenform/on-change fp p (set/difference (set value) (set [v]))]))}
                "close"]])
            [:span.empty-tags
             (when (and (not disable-errors) errors)
               {:style {:color "#db4040"}})
             (or (:placeholder props) "No tags added")])
          [:input {:value (:input @state)
                   :on-change #(swap! state assoc :input (.. % -target -value))
                   :on-focus (fn [e] (.addEventListener (.-target e) "keydown" on-keydown))
                   :on-blur (fn [e] (.removeEventListener (.-target e) "keydown" on-keydown))}]
          [:button {:on-click on-change} "Add"]]]))))
