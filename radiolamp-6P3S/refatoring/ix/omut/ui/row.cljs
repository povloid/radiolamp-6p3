(ns ix.omut.ui.row
  (:require [om.dom :as dom :include-macros true]))





(defn row [& body]
  (apply dom/div #js {:className "row"} body))
