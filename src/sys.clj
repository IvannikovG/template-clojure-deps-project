(ns sys
  (:require [clojure.java.shell :as shell]
            [cheshire.core]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn exec [& args]
  (println "Ex:" args)
  (println (apply shell/sh args)))

(defn bash [cmd]
  (let [res   (shell/sh "bash" "-c" cmd)]
    (println cmd)
    (println res)
    (:exit res)))

(defn bash! [cmd]
  (let [res (shell/sh "bash" "-c" cmd)]
    (if (= 0 (:exit res))
      (do (when (:err res) (println (:err res)))
        (:out res))
      (throw (Exception. (:err res))))))

(defn bash-out [cmd]
  ;; (println "Bash:" cmd)
  (shell/sh "bash" "-c" cmd))

(defn ensure-dir [dir]
  (when-not (.exists (io/file dir))
    (bash  (str "mkdir -p " dir))))
