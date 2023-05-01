(ns auth.subscribe
  (:require [routing :refer [handler validate-subscription]]
            [handler-helpers :as hh]
            [db :as db]
            [utils :as u]))

(defn create-subscription
  [ctx user-id]
  (let [valid-until (u/today-plus-days 30)
        subscription {:valid-until valid-until
                      :user-id     user-id
                      :status      "active"}
        created-subscription (db/save-resource!
                              ctx (-> subscription
                                     (merge {:resourceType "UserSubscription"})))]
    created-subscription))

(defmethod handler ::subscribe
  [ctx {:keys [headers cookies] :as _request}]
  (let [{:keys [id]}        (hh/get-token-claims headers cookies)
        user-subs-ids       (->> (db/query
                                  ctx {:h {:select :id
                                           :from [[:UserSubscription :s]]
                                           :where [:and
                                                   [:= (db/h>> :s.resource [:user-id]) id]]}})
                                 (map :s))
        _ (doseq [id user-subs-ids]
            (db/save! ctx {:resourceType "UserSubscription"
                           :id id
                           :status "inactive"}))
        subscription        (->> (db/query
                                  ctx {:h {:select [[:s.resource :s]]
                                           :from [[:UserSubscription :s]]
                                           :where [:and
                                                   [:= (db/h>> :s.resource [:user-id]) id]
                                                   [:= (db/h>> :s.resource [:status]) "active"]]}})
                                 (map :s))
        valid-subscription? (validate-subscription ctx subscription)]
    valid-subscription?
    (if valid-subscription?
      {:status 200
       :body {:message "Valid subscription exists"
              :already-created "true"
              :subscription subscription}}
      (let [new-subscription (create-subscription ctx id)]
        {:status 200
         :body {:message "Subscription created"
                :subscription new-subscription}}))))
