(ns r6p3s.ui.list-group
  (:require [om.dom :as dom :include-macros true]))


(defn render [list-groups-items & [class+]]
  (apply dom/ul #js {:className (str "list-group "
                                     (or class+ ""))
                     :style     #js {:marginTop 10}}
         list-groups-items))
