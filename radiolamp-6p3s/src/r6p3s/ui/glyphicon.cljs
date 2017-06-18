(ns r6p3s.ui.glyphicon
  (:require [om.dom :as dom :include-macros true]))

(defn render [name & [class+ size style]]
  (dom/span #js {:className   (str "glyphicon glyphicon-"
                                   name " " (or class+ ""))
                 :style       (if size
                                #js {:fontSize size}
                                style)
                 :aria-hidden "true"}))
