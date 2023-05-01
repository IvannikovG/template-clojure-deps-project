(ns rpc-methods
  (:require [db :as db]
            [clojure.walk :as walk]
            [clojure.java.io :as io]))


(defmulti rpc-handler (fn [_ctx body _schema] (-> body :method symbol)))

(defmethod rpc-handler 'rpc/test-rpc
  [_ctx body _schema]
  (let [{:keys [_method _jsonrpc _params id]} body]
    {:rpc-test-handler-worked "true"
     :body body}))

(defmethod rpc-handler 'rpc/test-rpc2
  [_ctx body _schema]
  (let [{:keys [_method _jsonrpc _params id]} body]
    {:rpc-test2-handler-worked "true"
     :body body}))
