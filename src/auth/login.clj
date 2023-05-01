(ns auth.login
  (:require [routing :refer [handler create-jwt-token]]
            [db :as db]
            [utils :as u]
            [clojure.string :as str]))

(defmethod handler ::login
  [ctx {:keys [body] :as _request}]
  (let [{:keys [password username]} body
        pw               (try (String. (.decode (java.util.Base64/getDecoder) password))
                              (catch Exception e ""))
        un               (try (str/lower-case (String. (.decode (java.util.Base64/getDecoder) username)))
                              (catch Exception e ""))
        user             (db/query-first ctx {:h {:select :*
                                                  :from :app_user
                                                  :where [:= (db/h>> :resource [:username]) un]}})
        user-res         (:resource user)
        passwords-match? (= (:password user-res) (try (u/md5 pw)
                                                      (catch Exception e false)))
        app-env          (-> ctx :config :app-env)]
    (cond (not (seq user)) {:status 400
                            :body {:message "No user with this username"}}

          passwords-match? (let [session {:id           (u/uuid)
                                          :resourceType "session"
                                          :user-id      (:id user)
                                          :expiration   (u/timestamp-from (u/day-from-now))}
                                 _       (db/save! ctx session)
                                 prep-user {:id (:id user)
                                            :username (:username user-res)}
                                 jwt-token (create-jwt-token ctx prep-user)]
                             (cond-> {:status 200
                                      :body {:jwt-token jwt-token}}
                               (= app-env "test") (merge {:cookies {:jwt-token jwt-token}})))

          :else {:status 400
                 :body {:message "Wrong password"}})))
