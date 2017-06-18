(ns r6p3s.ui.tbody
  (:require [om.dom :as dom :include-macros true]))

(defn render [trs]
  (apply dom/tbody nil trs))


