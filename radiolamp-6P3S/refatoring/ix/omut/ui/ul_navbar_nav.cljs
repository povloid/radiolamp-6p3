(ns ix.omut.ui.ul-navbar-nav
  (:require [om.dom :as dom :include-macros true]))


(defn render [& body]
  (apply dom/ul #js {:className "nav navbar-nav"}
         body))
