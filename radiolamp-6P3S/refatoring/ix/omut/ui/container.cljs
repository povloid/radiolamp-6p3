(ns ix.omut.ui.container
  (:require [om.dom :as dom :include-macros true]))


(defn render [& body]
  (apply dom/div #js {:className "container"} body))
