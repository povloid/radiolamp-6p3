(ns ix.omut.ui.container-fluid
  (:require [om.dom :as dom :include-macros true]))


(defn render [& body]
  (apply dom/div #js {:className "container-fluid"} body))
