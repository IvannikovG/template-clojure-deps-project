(ns auth.register
  (:require [routing :refer [handler]]
            [db :as db]
            [utils :as u]
            [clojure.string :as str]))

(defmethod handler ::register
  [ctx {:keys [body] :as _request}]
  (let [{:keys [password username email]} body
        u-name (str/lower-case (String. (.decode (java.util.Base64/getDecoder) username)))
        u-h-e (String. (.decode (java.util.Base64/getDecoder) email))
        pw (String. (.decode (java.util.Base64/getDecoder) password))
        user (db/query-first ctx {:dsql
                                  {:ql/type :pg/select
                                   :select :*
                                   :from {:t :app_user}
                                   :where ^:pg/op [:= [:jsonb/#>> :resource [:username]] u-name]}})]
    (if (seq user)
      {:status 409
       :body {:error "User exists"}}
      (let [hashed-password (u/md5 pw)
            body (db/save! ctx (assoc body
                                      :resourceType "app-user"
                                      :password hashed-password
                                      :email u-h-e
                                      :username u-name))]
        {:status 200
         :body body}))))
