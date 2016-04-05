(ns r6p3s.ui.container
  (:require [om.dom :as dom :include-macros true]))


(defn render [& body]
  (apply dom/div #js {:className "container"} body))
