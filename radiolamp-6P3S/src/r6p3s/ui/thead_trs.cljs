(ns r6p3s.ui.thead-trs
  (:require [om.dom :as dom :include-macros true]))


(defn render [trs]
  (->> trs
       (map (fn [ths]
              (apply dom/tr nil ths)))
       (apply dom/thead nil)))
