(ns r6p3s.ui.font-icon
  (:require [om.dom :as dom :include-macros true]))


(defn render [name & [size]]
  (dom/span #js {:className   name
                 :style       #js {:fontSize size}
                 :aria-hidden "true"}))
