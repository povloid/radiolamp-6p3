(ns r6p3s.ui.media-object
  (:require [om.dom :as dom :include-macros true]))



(defn render [{:keys [src class+ style]}]
  (dom/img #js {:className (str "media-object " (or class+ ""))
                :style     style
                :src       src}))
