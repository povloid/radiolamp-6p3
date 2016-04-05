(ns r6p3s.ui.thead-tr
  (:require [om.dom :as dom :include-macros true]))


(defn render [ths]
  (dom/thead nil (apply dom/tr nil ths)))
