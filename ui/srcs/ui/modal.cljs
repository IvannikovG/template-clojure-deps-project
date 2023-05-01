(ns ui.modal
  (:require
    [re-frame.core :as rf]))

(rf/reg-event-db
  ::modal
  (fn [db [_ props]]
    (assoc db :modal props)))

(rf/reg-event-db
  :modal/modal
  (fn [db [_ props]]
    (assoc db :modal props)))

(rf/reg-fx
  :modal/modal
  (fn [m]
    (rf/dispatch [::modal m])))

(rf/reg-event-db
  ::modal-close
  (fn [db _]
    (dissoc db :modal)))

(defn close [] (rf/dispatch [::modal-close]) )
(rf/reg-fx ::modal-close close )
(rf/reg-fx :modal/close close)

(rf/reg-sub
  ::modal
  (fn [db _]
    (get db :modal)))

(defn do-action-and-close [props]
  (rf/dispatch (:action props))
  (close))

(defn modal []
  (let [props (rf/subscribe [::modal])]
    (fn []
      (when @props
        [:div.modal-frame
         [:div.modal-overlay {:style {:z-index "1002" :display "block"}}]
         [:div#modal.modal.open {:style {:z-index "1003"
                                         :display "flex"
                                         :align-items "center"
                                         :justify-content "center"
                                         :opacity "0.9"
                                         :transform "scale(1)"}}
          [:div.modal-window {:style {:padding "20px"
                                      :border-radius "10px"
                                      :box-shadow "0px 0px 10px 1px rgba(0, 0, 0, 0.5)"
                                      :background-color "#f1f1f1"}}
           [:div.modal-content {:style {:align-items "center"
                                        :justify-content "center"
                                        :background-color "#f1f1f1"
                                        :border "0"}}
            [:h4 (or (:header @props) "Header")]
            [:h5 (:content @props)]]

           [:div.modal-footer {:style {:align-items "center" :justify-content "center"}}
            [:button.btn.btn-danger
             {:on-click #(do-action-and-close @props)}  (or (:action-label @props) "Yes")]
            [:button.btn.btn-primary
             {:on-click #(rf/dispatch [::modal-close])} (or (:close-label @props) "No")]
            ]]]]))))
