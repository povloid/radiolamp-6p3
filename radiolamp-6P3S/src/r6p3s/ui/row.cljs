(ns r6p3s.ui.row
  (:require [om.dom :as dom :include-macros true]))





(defn row [& body]
  (apply dom/div #js {:className "row"} body))
