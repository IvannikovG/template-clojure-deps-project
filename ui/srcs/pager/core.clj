(ns pager.core)

(load-file "ui/srcs/pager/core.cljc")

(defmacro reg-mw [key handler]
  `(reg-mw* ~key ~handler))

(defmacro reg-page [page]
  `(reg-page* ~(keyword (ns-name *ns*)) ~page))

(defmacro reg-page [page]
  `(reg-page* ~(keyword (ns-name *ns*)) ~page))

(defmacro reg-init [fn]
  `(reg-init* ~(keyword (ns-name *ns*)) ~fn))

(defmacro reg-event-db [& args]
  `(reg-event-db* ~(keyword (ns-name *ns*)) ~@args))

(defmacro reg-sub [& args]
  `(reg-sub* ~(keyword (ns-name *ns*)) ~@args))

(defmacro reg-event-fx [key fn]
  `(reg-event-fx* ~(keyword (ns-name *ns*)) ~key ~fn))
