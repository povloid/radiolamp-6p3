(ns ix.omut.ui.tbody
  (:require [om.dom :as dom :include-macros true]))

(defn render [trs]
  (apply dom/tbody nil trs))


