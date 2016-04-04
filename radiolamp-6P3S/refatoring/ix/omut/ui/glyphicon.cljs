(ns ix.omut.ui.glyphicon
  (:require [om.dom :as dom :include-macros true]))

(defn render [name & [class+ size]]
  (dom/span #js {:className   (str "glyphicon glyphicon-"
                                   name " " (or class+ ""))
                 :style       #js {:fontSize size}
                 :aria-hidden "true"}))
