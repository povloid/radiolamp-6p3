(ns ix.omut.ui.navbar-nav-right
  (:require [om.dom :as dom :include-macros true]))


(defn render [& body]
  (apply dom/ul #js {:className "nav navbar-nav navbar-right"}
         body))
