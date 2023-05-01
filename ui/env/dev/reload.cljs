(ns dev.reload
  (:require [ui.core :as ui]))

(defn re-render []
  (ui/mount-root))
